import http from 'k6/http';
import { check, sleep } from 'k6';

// Run with: k6 run k6-benchmark.js
export const options = {
    vus: 10,           // Number of virtual users
    duration: '30s',   // Duration of the test
};

const LOT_ID = '10000000-0000-0000-0000-000000000001';
const BASE_URL = 'http://localhost:8080/api/v1';

export function setup() {
    // Log in once and get token for all VUs
    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: 'admin@parkflow.com',
        password: 'password'
    }), {
        headers: { 'Content-Type': 'application/json' }
    });
    return { token: loginRes.json('token') };
}

export default function (data) {
    // Randomize time window to hit different cache keys / DB ranges
    const offsetMin = Math.floor(Math.random() * 60) + 10;
    const FROM_TIME = new Date(Date.now() + offsetMin * 60 * 1000).toISOString();
    const TO_TIME = new Date(Date.now() + (offsetMin + 60) * 60 * 1000).toISOString();
    
    // 1. Get availability
    const availRes = http.get(`${BASE_URL}/parking-lots/${LOT_ID}/availability?from=${FROM_TIME}&to=${TO_TIME}`);
    check(availRes, {
        'availability is 200': (r) => r.status === 200,
    });
    
    // 2. Book a random spot
    const spots = availRes.json();
    if (spots && spots.length > 0) {
        const availableSpots = spots.filter(s => s.isAvailable);
        if (availableSpots.length > 0) {
            const spotToBook = availableSpots[Math.floor(Math.random() * availableSpots.length)];
            const idempotencyKey = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
            const bookRes = http.post(`${BASE_URL}/reservations`, JSON.stringify({
                spotId: spotToBook.spotId,
                from: FROM_TIME,
                to: TO_TIME,
                licensePlate: 'AA1234BB'
            }), {
                headers: { 
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${data.token}`,
                    'Idempotency-Key': idempotencyKey
                }
            });
            check(bookRes, {
                'booking is 201, 400 (overlap), or 5xx (chaos)': (r) => r.status === 201 || r.status === 400 || r.status >= 500,
            });
        }
    }
    
    sleep(0.5);
}
