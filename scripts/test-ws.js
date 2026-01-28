const WebSocket = require('ws');
const http = require('http');

// Get token first
const data = JSON.stringify({username: 'admin', password: 'admin'});
const req = http.request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
}, (res) => {
    let body = '';
    res.on('data', chunk => body += chunk);
    res.on('end', () => {
        const token = JSON.parse(body).token;
        console.log('Got token:', token.substring(0, 20) + '...');
        
        // Try WebSocket connection to container 107
        const wsUrl = `ws://localhost:8080/containers/107/commands?token=${token}`;
        console.log('Connecting to:', wsUrl.substring(0, 60) + '...');
        
        const ws = new WebSocket(wsUrl);
        ws.on('open', () => {
            console.log('Connected!');
            ws.send(JSON.stringify({
                commandName: 'spawn',
                matchId: 1,
                playerId: 1,
                parameters: { matchId: 1, playerId: 1, entityType: 100 }
            }));
        });
        ws.on('message', (data) => {
            console.log('Received:', data.toString());
            ws.close();
            process.exit(0);
        });
        ws.on('error', (err) => {
            console.error('WebSocket Error:', err.message);
            process.exit(1);
        });
        ws.on('close', (code, reason) => {
            console.log('Closed:', code, reason.toString());
        });
        
        setTimeout(() => { console.log('Timeout'); process.exit(1); }, 5000);
    });
});
req.write(data);
req.end();
