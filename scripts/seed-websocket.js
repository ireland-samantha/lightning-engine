#!/usr/bin/env node
/**
 * High-performance seed script using WebSockets
 * Now with parallel container/match creation
 * Usage: node seed-websocket.js [config.json]
 */

const WebSocket = require('ws');
const http = require('http');
const https = require('https');

// Configuration
const API_URL = process.env.API_URL || 'http://localhost:8080';
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASS = process.env.ADMIN_PASS || 'admin';
const CONFIG_FILE = process.argv[2] || './seed-config.json';
const CONTAINER_CONCURRENCY = parseInt(process.env.CONTAINER_CONCURRENCY || '10', 10);
const MATCH_CONCURRENCY = parseInt(process.env.MATCH_CONCURRENCY || '20', 10);
const COMMANDS_PER_SECOND = parseInt(process.env.COMMANDS_PER_SECOND || '80', 10); // Rate limit

// Load config
const fs = require('fs');
const path = require('path');
const configPath = path.resolve(CONFIG_FILE);
if (!fs.existsSync(configPath)) {
    console.error(`Config file not found: ${configPath}`);
    process.exit(1);
}
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

// HTTP helper with connection pooling
const httpAgent = new http.Agent({ keepAlive: true, maxSockets: 50 });
const httpsAgent = new https.Agent({ keepAlive: true, maxSockets: 50 });

function httpRequest(method, endpoint, data = null) {
    return new Promise((resolve, reject) => {
        const url = new URL(endpoint, API_URL);
        const isHttps = url.protocol === 'https:';
        const lib = isHttps ? https : http;

        const options = {
            hostname: url.hostname,
            port: url.port || (isHttps ? 443 : 80),
            path: url.pathname + url.search,
            method: method,
            agent: isHttps ? httpsAgent : httpAgent,
            headers: {
                'Content-Type': 'application/json',
            }
        };

        if (global.token) {
            options.headers['Authorization'] = `Bearer ${global.token}`;
        }

        const req = lib.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch {
                    resolve(body);
                }
            });
        });

        req.on('error', reject);
        if (data) req.write(JSON.stringify(data));
        req.end();
    });
}

// Random number in range
function randomRange(min, max) {
    return min + Math.random() * (max - min);
}

// Get value from config
function getValue(cfg, index) {
    if (typeof cfg === 'number') return cfg;
    if (cfg.random) return randomRange(cfg.min || 0, cfg.max || 100);
    const start = cfg.start || cfg.value || 0;
    const increment = cfg.increment || 0;
    return start + (index * increment);
}

// Run promises with concurrency limit
async function parallelLimit(tasks, limit) {
    const results = [];
    const executing = new Set();

    for (const task of tasks) {
        const p = Promise.resolve().then(() => task());
        results.push(p);
        executing.add(p);

        const clean = () => executing.delete(p);
        p.then(clean, clean);

        if (executing.size >= limit) {
            await Promise.race(executing);
        }
    }

    return Promise.all(results);
}

// WebSocket command sender with response tracking and rate limiting
class CommandSender {
    constructor(containerId, token) {
        this.containerId = containerId;
        this.token = token;
        this.ws = null;
        this.connected = false;
        this.sentCount = 0;
        this.errorCount = 0;
        this.pendingResolves = []; // Queue of resolve functions waiting for responses
        this.lastSendTime = 0;
        this.minInterval = 1000 / COMMANDS_PER_SECOND; // ms between commands
    }

    connect() {
        return new Promise((resolve, reject) => {
            const wsUrl = API_URL.replace('http', 'ws') + `/containers/${this.containerId}/commands?token=${this.token}`;
            this.ws = new WebSocket(wsUrl);

            this.ws.on('open', () => {
                this.connected = true;
                resolve();
            });

            this.ws.on('message', (data) => {
                try {
                    const response = JSON.parse(data.toString());
                    if (response.status === 'ERROR') {
                        this.errorCount++;
                    }
                    // Resolve the oldest pending promise with the response
                    if (this.pendingResolves.length > 0) {
                        const resolveFn = this.pendingResolves.shift();
                        resolveFn(response);
                    }
                } catch (e) {
                    // Ignore parse errors
                }
            });

            this.ws.on('error', (err) => {
                reject(err);
            });

            this.ws.on('close', () => {
                this.connected = false;
            });
        });
    }

    // Fire-and-forget send with rate limiting
    async send(command) {
        if (!this.connected || this.ws.readyState !== WebSocket.OPEN) {
            return;
        }

        // Throttle to stay under rate limit
        const now = Date.now();
        const elapsed = now - this.lastSendTime;
        if (elapsed < this.minInterval) {
            await new Promise(r => setTimeout(r, this.minInterval - elapsed));
        }

        this.ws.send(JSON.stringify(command));
        this.lastSendTime = Date.now();
        this.sentCount++;
    }

    // Send and wait for response
    sendAndWait(command, timeoutMs = 5000) {
        return new Promise((resolve, reject) => {
            if (!this.connected || this.ws.readyState !== WebSocket.OPEN) {
                reject(new Error('WebSocket not connected'));
                return;
            }

            const timeout = setTimeout(() => {
                // Remove from pending queue
                const idx = this.pendingResolves.indexOf(resolveFn);
                if (idx >= 0) this.pendingResolves.splice(idx, 1);
                reject(new Error('Command timeout'));
            }, timeoutMs);

            const resolveFn = (response) => {
                clearTimeout(timeout);
                resolve(response);
            };

            this.pendingResolves.push(resolveFn);
            this.ws.send(JSON.stringify(command));
            this.sentCount++;
        });
    }

    close() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Progress tracking
const progress = {
    containersCreated: 0,
    containersStarted: 0,
    matchesCreated: 0,
    commandsSent: 0,
    errors: 0,
    totalContainers: 0,
    totalMatches: 0,
};

function printProgress() {
    process.stdout.write(
        `\rContainers: ${progress.containersStarted}/${progress.totalContainers} | ` +
        `Matches: ${progress.matchesCreated}/${progress.totalMatches} | ` +
        `Commands: ${progress.commandsSent} | ` +
        `Errors: ${progress.errors}   `
    );
}

// Process a single container
async function processContainer(containerIndex, numContainers, numMatches, modules, playerName, entityGroups) {
    const containerName = `${config.container?.name || 'seed'}-${containerIndex + 1}-${Date.now()}`;

    try {
        // Create container
        const containerResponse = await httpRequest('POST', '/api/containers', { name: containerName });
        const containerId = containerResponse.id;
        if (!containerId) {
            console.error(`\nFailed to create container: ${JSON.stringify(containerResponse)}`);
            progress.errors++;
            return null;
        }
        progress.containersCreated++;
        printProgress();

        // Start container
        await httpRequest('POST', `/api/containers/${containerId}/start`);

        // Wait for container to be running (with timeout)
        let isRunning = false;
        for (let attempt = 0; attempt < 60; attempt++) {
            await new Promise(r => setTimeout(r, 500));
            const status = await httpRequest('GET', `/api/containers/${containerId}`);
            if (status.status === 'RUNNING') {
                isRunning = true;
                break;
            }
        }
        if (!isRunning) {
            console.error(`\nContainer ${containerId} failed to start`);
            progress.errors++;
            return containerId;
        }
        progress.containersStarted++;
        printProgress();

        // Create player
        const playerResponse = await httpRequest('POST', `/api/containers/${containerId}/players`, { name: playerName });
        const playerId = playerResponse.id || 1;

        // Connect WebSocket
        const sender = new CommandSender(containerId, global.token);
        try {
            await sender.connect();
        } catch (wsErr) {
            console.error(`\nWebSocket connect failed for container ${containerId}: ${wsErr.message}`);
            progress.errors++;
            return containerId;
        }

        // Create matches sequentially - entity IDs are container-global
        let containerEntityId = 0;
        for (let m = 0; m < numMatches; m++) {
            const matchResponse = await httpRequest('POST', `/api/containers/${containerId}/matches`, {
                enabledModuleNames: modules
            });
            const matchId = matchResponse.id;
            if (!matchId) {
                if (progress.errors < 5) {
                    console.error(`\nMatch create failed: ${JSON.stringify(matchResponse)}`);
                }
                progress.errors++;
                continue;
            }
            progress.matchesCreated++;
            printProgress();

            // Spawn commands for this match
            // Entity IDs are container-global and sequential, starting at 1
            for (const group of entityGroups) {
                const count = group.count || 1;
                const entityType = group.entityType || 100;
                const hasRigidBody = !!group.rigidBody;

                for (let i = 0; i < count; i++) {
                    containerEntityId++;
                    const posX = Math.round(getValue(group.position?.x, i));
                    const posY = Math.round(getValue(group.position?.y, i));

                    // Spawn command
                    await sender.send({
                        commandName: 'spawn',
                        matchId: matchId,
                        playerId: playerId,
                        spawn: {
                            entityType: entityType,
                            positionX: posX,
                            positionY: posY
                        }
                    });

                    // Attach rigid body using predicted entity ID
                    if (hasRigidBody) {
                        await sender.send({
                            commandName: 'attachRigidBody',
                            matchId: matchId,
                            playerId: playerId,
                            attachRigidBody: {
                                entityId: containerEntityId,
                                mass: Math.round(group.rigidBody?.mass || 1),
                                positionX: posX,
                                positionY: posY,
                                velocityX: Math.round(getValue(group.rigidBody?.velocity?.x, i)),
                                velocityY: Math.round(getValue(group.rigidBody?.velocity?.y, i))
                            }
                        });
                    }
                }
            }
        }

        progress.commandsSent += sender.sentCount;
        progress.errors += sender.errorCount;
        printProgress();

        // Start auto-tick
        await httpRequest('POST', `/api/containers/${containerId}/play?intervalMs=1`);

        sender.close();
        return containerId;
    } catch (err) {
        progress.errors++;
        printProgress();
        return null;
    }
}

async function main() {
    console.log(`Using config: ${CONFIG_FILE}`);
    console.log(`Concurrency: ${CONTAINER_CONCURRENCY} containers, rate limit: ${COMMANDS_PER_SECOND} cmd/s\n`);

    // Authenticate
    console.log('Authenticating...');
    const loginResponse = await httpRequest('POST', '/api/auth/login', {
        username: ADMIN_USER,
        password: ADMIN_PASS
    });

    if (!loginResponse.token) {
        console.error('Authentication failed:', loginResponse);
        process.exit(1);
    }
    global.token = loginResponse.token;
    console.log('Authenticated successfully\n');

    // Parse config
    const numContainers = config.containers || 1;
    const numMatches = config.matchesPerContainer || 1;
    const modules = config.match?.modules || ['EntityModule', 'RigidBodyModule', 'GridMapModule'];
    const playerName = config.player?.name || 'SeedPlayer';
    const entityGroups = config.entities || [];

    // Calculate totals
    let entitiesPerMatch = 0;
    for (const group of entityGroups) {
        entitiesPerMatch += group.count || 1;
    }
    const totalMatches = numContainers * numMatches;
    const totalEntities = totalMatches * entitiesPerMatch;

    progress.totalContainers = numContainers;
    progress.totalMatches = totalMatches;

    console.log(`Plan: ${numContainers} containers x ${numMatches} matches x ${entitiesPerMatch} entities = ${totalEntities} total entities\n`);

    const startTime = Date.now();

    // Create container tasks
    const containerTasks = [];
    for (let c = 0; c < numContainers; c++) {
        containerTasks.push(() => processContainer(c, numContainers, numMatches, modules, playerName, entityGroups));
    }

    // Run containers in parallel with limit
    const containerIds = await parallelLimit(containerTasks, CONTAINER_CONCURRENCY);
    const validContainerIds = containerIds.filter(id => id !== null);

    const duration = Math.round((Date.now() - startTime) / 1000);

    console.log('\n\n========================================');
    console.log('       Seed Complete!');
    console.log('========================================\n');
    console.log(`Containers:        ${validContainerIds.length}/${numContainers}`);
    console.log(`Matches/Container: ${numMatches}`);
    console.log(`Entities/Match:    ${entitiesPerMatch}`);
    console.log(`Total Entities:    ${totalEntities}`);
    console.log(`Commands Sent:     ${progress.commandsSent}`);
    console.log(`Errors:            ${progress.errors}`);
    console.log(`Duration:          ${duration}s`);
    console.log(`Throughput:        ${Math.round(progress.commandsSent / duration)} cmd/s`);
    console.log(`\nContainer IDs: ${validContainerIds.slice(0, 10).join(', ')}${validContainerIds.length > 10 ? '...' : ''}`);
    console.log('\nNote: Containers are auto-ticking to process queued commands.');
}

main().catch(err => {
    console.error('\nError:', err);
    process.exit(1);
});
