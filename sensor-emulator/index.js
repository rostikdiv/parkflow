const express = require('express');
const cors = require('cors');
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;
const API_BASE_URL = process.env.API_BASE_URL || 'http://host.docker.internal:8080/api/v1';
const INTERNAL_API_URL = process.env.INTERNAL_API_URL || 'http://host.docker.internal:8080/api/internal/v1/sensor-events';

const BATCH_INTERVAL_MS = parseInt(process.env.BATCH_INTERVAL_MS || '3000', 10);
const CLIENT_SIM_INTERVAL_MS = parseInt(process.env.CLIENT_SIM_INTERVAL_MS || '10000', 10);
const AUTO_START = process.env.AUTO_START !== 'false'; // Default to true for backward compatibility

const STATUSES = ['FREE', 'OCCUPIED', 'UNKNOWN'];
let allSpots = []; // Will store { id, lotId, status, lastUpdate }
let authToken = null; // JWT token

let isRunning = false;
let sensorBatchTimer = null;
let clientBookingsTimer = null;

// Authenticate emulator
async function authenticate() {
    console.log('Authenticating emulator...');
    try {
        const credentials = {
            email: 'emulator@parkflow.com',
            password: 'password',
            fullName: 'Sensor Emulator',
            phone: '+380990000000'
        };
        // Try to register
        try {
            await axios.post(`${API_BASE_URL}/auth/register`, credentials);
        } catch (e) {
            // Ignore if already exists
        }
        // Login
        const res = await axios.post(`${API_BASE_URL}/auth/login`, {
            email: credentials.email,
            password: credentials.password
        });
        authToken = res.data.token;
        console.log('Emulator authenticated successfully.');
        return true;
    } catch (error) {
        console.error('Failed to authenticate emulator:', error.message);
        return false;
    }
}

// Initialize: Fetch all spots
async function fetchAllSpots() {
    console.log(`Fetching parking lots from ${API_BASE_URL}...`);
    try {
        const geoResponse = await axios.get(`${API_BASE_URL}/parking-lots/geojson`);
        const lots = geoResponse.data.features;
        console.log(`Found ${lots.length} parking lots.`);

        allSpots = []; // Clear array
        for (const lot of lots) {
            const lotId = lot.properties.id;
            const spotsResponse = await axios.get(`${API_BASE_URL}/parking-lots/${lotId}/spots`);
            const spots = spotsResponse.data;
            
            spots.forEach(spot => {
                allSpots.push({
                    spotId: spot.id,
                    lotId: lotId,
                    status: 'FREE',
                    lastUpdate: Date.now()
                });
            });
        }
        console.log(`Loaded a total of ${allSpots.length} spots for simulation.`);
        return true;
    } catch (error) {
        console.error('Failed to fetch spots:', error.message);
        return false;
    }
}

// Sensor Simulator Logic
async function sendSensorBatch() {
    if (allSpots.length === 0) return;

    const batchSize = Math.min(allSpots.length, Math.floor(Math.random() * 20) + 5);
    const events = [];

    for (let i = 0; i < batchSize; i++) {
        const spotIndex = Math.floor(Math.random() * allSpots.length);
        const spot = allSpots[spotIndex];

        if (Math.random() < 0.01) continue; 
        if (Math.random() < 0.0005) {
            spot.status = STATUSES[Math.floor(Math.random() * 2)];
        }
        spot.lastUpdate = Date.now();
        
        events.push({
            externalEventId: uuidv4(),
            spotId: spot.spotId,
            lotId: spot.lotId,
            status: spot.status,
            timestamp: new Date().toISOString()
        });
    }

    if (events.length === 0) return;

    try {
        await axios.post(INTERNAL_API_URL, { events });
        console.log(`[${new Date().toISOString()}] Sent sensor batch of ${events.length} events.`);
    } catch (e) {
        console.error(`[${new Date().toISOString()}] Failed to send sensor batch: ${e.message}`);
    }
}

// Client Simulator Logic (Bookings)
async function simulateClientBookings() {
    if (allSpots.length === 0) return;

    const bookingsCount = Math.floor(Math.random() * 5) + 1;

    for (let i = 0; i < bookingsCount; i++) {
        const spot = allSpots[Math.floor(Math.random() * allSpots.length)];
        const now = new Date();
        const rand = Math.random();
        
        let offsetMs;
        if (rand < 0.2) offsetMs = (24 * 60 * 60 * 1000) + Math.floor(Math.random() * 24 * 60 * 60 * 1000);
        else if (rand < 0.5) offsetMs = (1 * 60 * 60 * 1000) + Math.floor(Math.random() * 4 * 60 * 60 * 1000);
        else offsetMs = Math.floor(Math.random() * 5 * 60 * 1000) + (1 * 60 * 1000);

        const fromTimeObj = new Date(now.getTime() + offsetMs);
        const durationMs = (30 * 60 * 1000) + Math.floor(Math.random() * 2.5 * 60 * 60 * 1000);
        const toTimeObj = new Date(fromTimeObj.getTime() + durationMs);

        const isCustom = Math.random() < 0.2;
        let generatedPlate;
        if (isCustom) {
            generatedPlate = 'SIM' + Math.floor(Math.random() * 99999).toString();
        } else {
            const plateLetters = ['AA', 'BC', 'KA', 'CE', 'AI'];
            const randomPrefix = plateLetters[Math.floor(Math.random() * plateLetters.length)];
            const randomSuffix = plateLetters[Math.floor(Math.random() * plateLetters.length)];
            const randomNumbers = Math.floor(Math.random() * 9999).toString().padStart(4, '0');
            generatedPlate = `${randomPrefix}${randomNumbers}${randomSuffix}`;
        }

        const payload = {
            spotId: spot.spotId,
            from: fromTimeObj.toISOString(),
            to: toTimeObj.toISOString(),
            licensePlate: generatedPlate
        };

        try {
            await axios.post(`${API_BASE_URL}/reservations`, payload, {
                headers: {
                    'Idempotency-Key': uuidv4(),
                    'Authorization': `Bearer ${authToken}`
                }
            });
            console.log(`[${new Date().toISOString()}] Client successfully booked spot ${spot.spotId} (${payload.licensePlate})`);
            
            if (offsetMs < 2 * 60 * 60 * 1000) {
                if (Math.random() < 0.98) {
                    let arrivalDelay = Math.max(0, offsetMs + (Math.random() * 60000 - 30000));
                    setTimeout(() => {
                        spot.status = 'OCCUPIED';
                        console.log(`[Simulator] Car ${payload.licensePlate} physically arrived at ${spot.spotId}`);
                    }, arrivalDelay);
                }
            }
        } catch (e) {
            if (e.response && e.response.status !== 409) {
                console.error(`[${new Date().toISOString()}] Client booking failed: ${e.message}`);
            }
        }
    }
}

// Controller functions
async function startSimulation() {
    if (isRunning) return false;
    
    let authSuccess = await authenticate();
    if (!authSuccess) return false;

    let spotsSuccess = await fetchAllSpots();
    if (!spotsSuccess) return false;

    console.log('Starting simulation timers...');
    sensorBatchTimer = setInterval(sendSensorBatch, BATCH_INTERVAL_MS);
    clientBookingsTimer = setInterval(simulateClientBookings, CLIENT_SIM_INTERVAL_MS);
    isRunning = true;
    return true;
}

function stopSimulation() {
    if (!isRunning) return false;
    
    console.log('Stopping simulation timers...');
    clearInterval(sensorBatchTimer);
    clearInterval(clientBookingsTimer);
    sensorBatchTimer = null;
    clientBookingsTimer = null;
    isRunning = false;
    return true;
}

// Express API
app.get('/actuator/health', (req, res) => {
    res.status(200).json({ status: 'UP' });
});

app.get('/api/emulator/status', (req, res) => {
    res.json({ isRunning, activeSpotsCount: allSpots.length });
});

app.post('/api/emulator/start', async (req, res) => {
    if (isRunning) {
        return res.status(400).json({ message: 'Simulation is already running.' });
    }
    const success = await startSimulation();
    if (success) {
        res.json({ message: 'Simulation started.' });
    } else {
        res.status(500).json({ message: 'Failed to start simulation. Check logs.' });
    }
});

app.post('/api/emulator/stop', (req, res) => {
    if (!isRunning) {
        return res.status(400).json({ message: 'Simulation is not running.' });
    }
    stopSimulation();
    res.json({ message: 'Simulation stopped.' });
});

// Start Server
app.listen(PORT, () => {
    console.log(`Emulator Cloud Run service listening on port ${PORT}`);
    
    if (AUTO_START) {
        console.log('AUTO_START is enabled. Starting simulation in 5 seconds...');
        setTimeout(() => startSimulation(), 5000);
    }
});
