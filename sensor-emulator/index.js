const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

const API_BASE_URL = process.env.API_BASE_URL || 'http://host.docker.internal:8080/api/v1';
const INTERNAL_API_URL = process.env.INTERNAL_API_URL || 'http://host.docker.internal:8080/api/internal/v1/sensor-events';

const BATCH_INTERVAL_MS = parseInt(process.env.BATCH_INTERVAL_MS || '3000', 10);
const CLIENT_SIM_INTERVAL_MS = parseInt(process.env.CLIENT_SIM_INTERVAL_MS || '10000', 10);

const STATUSES = ['FREE', 'OCCUPIED', 'UNKNOWN'];
let allSpots = []; // Will store { id, lotId, status, lastUpdate }

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

        // 5% chance to simulate a broken sensor (don't update it)
        if (Math.random() < 0.05) {
            continue; 
        }

        // 20% chance to change status, otherwise keep it
        if (Math.random() < 0.2) {
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
        const fromTimeObj = new Date(now.getTime() + 60 * 1000); // from 1 minute in future
        const toTimeObj = new Date(fromTimeObj.getTime() + 60 * 60 * 1000); // 1 hour duration

        const payload = {
            spotId: spot.spotId,
            from: fromTimeObj.toISOString(),
            to: toTimeObj.toISOString(),
            licensePlate: 'SIM' + Math.floor(Math.random() * 9999).toString().padStart(4, '0')
        };

        try {
            await axios.post(`${API_BASE_URL}/reservations`, payload, {
                headers: {
                    'Idempotency-Key': uuidv4()
                }
            });
            console.log(`[${new Date().toISOString()}] Client successfully booked spot ${spot.spotId} (${payload.licensePlate})`);
            
            // Introduce intentional anomaly: 30% chance the "car" actually arrives and triggers the sensor to OCCUPIED
            if (Math.random() < 0.3) {
                setTimeout(() => {
                    spot.status = 'OCCUPIED';
                    console.log(`[Simulator] Car ${payload.licensePlate} physically arrived at ${spot.spotId}`);
                }, 10000); // arrives after 10 seconds
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
fetchAllSpots().then(() => {
    setInterval(sendSensorBatch, BATCH_INTERVAL_MS);
    setInterval(simulateClientBookings, CLIENT_SIM_INTERVAL_MS);
});
