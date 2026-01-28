const WebSocket = require('ws');
const http = require('http');

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
        const ws = new WebSocket(`ws://localhost:8080/containers/1/commands?token=${token}`);
        let msgCount = 0;
        ws.on('open', () => {
            console.log('Connected');
            ws.send(JSON.stringify({
                commandName: 'spawn',
                matchId: 1,
                playerId: 1,
                parameters: { matchId: 1, playerId: 1, entityType: 100 }
            }));
        });
        ws.on('message', (data) => {
            msgCount++;
            console.log('Response', msgCount + ':', data.toString());
            if (msgCount >= 2) {
                ws.close();
                process.exit(0);
            }
        });
        ws.on('error', (err) => console.error('Error:', err.message));
        setTimeout(() => { console.log('Timeout after', msgCount, 'messages'); process.exit(0); }, 3000);
    });
});
req.write(data);
req.end();
