const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

const API_BASE_URL = process.env.API_BASE_URL || 'http://host.docker.internal:8080/api/v1';
const INTERNAL_API_URL = process.env.INTERNAL_API_URL || 'http://host.docker.internal:8080/api/internal/v1/sensor-events';

const BATCH_INTERVAL_MS = parseInt(process.env.BATCH_INTERVAL_MS || '3000', 10);
const CLIENT_SIM_INTERVAL_MS = parseInt(process.env.CLIENT_SIM_INTERVAL_MS || '10000', 10);

const STATUSES = ['FREE', 'OCCUPIED', 'UNKNOWN'];
let allSpots = []; // Will store { id, lotId, status, lastUpdate }
let authToken = null; // JWT token

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
    } catch (error) {
        console.error('Failed to authenticate emulator. Retrying in 5s...', error.message);
        setTimeout(authenticate, 5000);
    }
}

// Initialize: Fetch all spots
async function fetchAllSpots() {
    console.log(`Fetching parking lots from ${API_BASE_URL}/parking-lots/geojson...`);
    try {
        const geoResponse = await axios.get(`${API_BASE_URL}/parking-lots/geojson`);
        const lots = geoResponse.data.features;
        console.log(`Found ${lots.length} parking lots.`);

        for (const lot of lots) {
            const lotId = lot.properties.id;
            console.log(`Fetching spots for lot ${lotId}...`);
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
    } catch (error) {
        console.error('Failed to fetch spots. Backend might not be ready yet. Retrying in 5 seconds...', error.message);
        setTimeout(fetchAllSpots, 5000);
    }
}

// Sensor Simulator Logic
async function sendSensorBatch() {
    if (allSpots.length === 0) return;

    const batchSize = Math.min(allSpots.length, Math.floor(Math.random() * 20) + 5); // 5 to 25 events
    const events = [];

    for (let i = 0; i < batchSize; i++) {
        // Pick a random spot
        const spotIndex = Math.floor(Math.random() * allSpots.length);
        const spot = allSpots[spotIndex];

        // 1% chance to simulate a broken sensor (don't update it)
        if (Math.random() < 0.01) {
            continue; 
        }

        // 0.05% chance to change status randomly per tick (simulates unbooked cars parking, creates OCCUPIED_WITHOUT_RESERVATION anomaly)
        if (Math.random() < 0.0005) {
            spot.status = STATUSES[Math.floor(Math.random() * 2)]; // Only FREE or OCCUPIED (UNKNOWN is set by backend)
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

    // Simulate 1 to 5 users booking spots simultaneously
    const bookingsCount = Math.floor(Math.random() * 5) + 1;

    for (let i = 0; i < bookingsCount; i++) {
        const spot = allSpots[Math.floor(Math.random() * allSpots.length)];
        
        const now = new Date();
        const rand = Math.random();
        
        let offsetMs;
        if (rand < 0.2) {
            // 20% chance: tomorrow (24 to 48 hours in the future)
            offsetMs = (24 * 60 * 60 * 1000) + Math.floor(Math.random() * 24 * 60 * 60 * 1000);
        } else if (rand < 0.5) {
            // 30% chance: later today (1 to 5 hours in the future)
            offsetMs = (1 * 60 * 60 * 1000) + Math.floor(Math.random() * 4 * 60 * 60 * 1000);
        } else {
            // 50% chance: almost immediately (1 to 5 minutes in the future)
            offsetMs = Math.floor(Math.random() * 5 * 60 * 1000) + (1 * 60 * 1000);
        }

        const fromTimeObj = new Date(now.getTime() + offsetMs);
        
        // Random duration between 30 minutes and 3 hours
        const durationMs = (30 * 60 * 1000) + Math.floor(Math.random() * 2.5 * 60 * 60 * 1000);
        const toTimeObj = new Date(fromTimeObj.getTime() + durationMs);

        // Generate standard (e.g. AA1234BB) or custom (e.g. VIP1) Ukrainian plate
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
            
            // Only schedule physical arrival if the booking is happening within the next 2 hours
            // (to avoid keeping thousands of setTimeouts in memory for tomorrow's bookings)
            if (offsetMs < 2 * 60 * 60 * 1000) {
                // Normal behavior: 98% chance the "car" actually arrives around the start time.
                // 2% chance it's a no-show (creates RESERVED_BUT_EMPTY anomaly)
                if (Math.random() < 0.98) {
                    // Car arrives slightly before or after the booking starts (offsetMs +/- 30 seconds)
                    let arrivalDelay = offsetMs + (Math.random() * 60000 - 30000);
                    arrivalDelay = Math.max(0, arrivalDelay); // Execute immediately if negative
                    
                    setTimeout(() => {
                        spot.status = 'OCCUPIED';
                        console.log(`[Simulator] Car ${payload.licensePlate} physically arrived at ${spot.spotId}`);
                    }, arrivalDelay);
                }
            }

        } catch (e) {
            // Ignore 409 Conflict if spot is already booked
            if (e.response && e.response.status !== 409) {
                console.error(`[${new Date().toISOString()}] Client booking failed: ${e.message}`);
            }
        }
    }
}

// Start Simulator
authenticate().then(() => {
    fetchAllSpots().then(() => {
        setInterval(sendSensorBatch, BATCH_INTERVAL_MS);
        setInterval(simulateClientBookings, CLIENT_SIM_INTERVAL_MS);
    });
});
