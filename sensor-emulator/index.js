const http = require('http');
const { v4: uuidv4 } = require('uuid');

const API_URL = process.env.API_URL || 'http://localhost:8080/api/internal/v1/sensor-events';
const BATCH_INTERVAL_MS = parseInt(process.env.BATCH_INTERVAL_MS || '5000', 10);

// We hardcode some spot IDs and their lot IDs from V3__seed_data.sql for simulation purposes.
const KNOWN_SPOTS = [
    { lotId: '10000000-0000-0000-0000-000000000001', spotId: '00000000-0000-0000-0000-000000000001' },
    { lotId: '10000000-0000-0000-0000-000000000001', spotId: '00000000-0000-0000-0000-000000000002' },
    { lotId: '10000000-0000-0000-0000-000000000002', spotId: '00000000-0000-0000-0000-000000000011' },
];

const STATUSES = ['FREE', 'OCCUPIED', 'UNKNOWN'];

function generateRandomEvent() {
    const spot = KNOWN_SPOTS[Math.floor(Math.random() * KNOWN_SPOTS.length)];
    return {
        externalEventId: uuidv4(),
        spotId: spot.spotId,
        lotId: spot.lotId,
        status: STATUSES[Math.floor(Math.random() * STATUSES.length)],
        timestamp: new Date().toISOString()
    };
}

function sendBatch() {
    const batchSize = Math.floor(Math.random() * 3) + 1; // 1 to 3 events per batch
    const events = [];
    for (let i = 0; i < batchSize; i++) {
        events.push(generateRandomEvent());
    }

    const payload = JSON.stringify({ events });
    const url = new URL(API_URL);

    const options = {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(payload)
        }
    };

    const req = http.request(options, (res) => {
        console.log(`[${new Date().toISOString()}] Sent batch of ${batchSize} events. Response status: ${res.statusCode}`);
        res.on('data', () => {}); // Consume data to free memory
    });

    req.on('error', (e) => {
        console.error(`[${new Date().toISOString()}] Failed to send batch: ${e.message}`);
    });

    req.write(payload);
    req.end();
}

console.log(`Starting sensor emulator. Target API: ${API_URL}, Interval: ${BATCH_INTERVAL_MS}ms`);
setInterval(sendBatch, BATCH_INTERVAL_MS);
