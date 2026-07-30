import http from 'k6/http';
import { check, sleep } from 'k6';

// Run with: k6 run k6-benchmark.js
// Configure options
export const options = {
    vus: 50,           // Number of virtual users
    duration: '30s',   // Duration of the test
};

// Assuming the first parking lot ID from V3__seed_data.sql is used
const LOT_ID = '9580b06b-7cb8-4db8-8314-87d4bf09c061';
const FROM_TIME = new Date(Date.now() + 3600 * 1000).toISOString(); // +1 hour
const TO_TIME = new Date(Date.now() + 7200 * 1000).toISOString();   // +2 hours

export default function () {
    const url = `http://localhost:8080/api/v1/parking-lots/${LOT_ID}/availability?from=${FROM_TIME}&to=${TO_TIME}`;
    
    // We send a request to the availability endpoint
    const res = http.get(url);
    
    // Assertions
    check(res, {
        'is status 200': (r) => r.status === 200,
        'response is fast (< 200ms)': (r) => r.timings.duration < 200,
    });
    
    // Small pause between requests
    sleep(0.1);
}
