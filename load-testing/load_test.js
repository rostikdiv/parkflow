import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'https://parkflow-backend-258044247462.us-central1.run.app/api/v1';

// Допоміжна функція замість k6/crypto
function randomStr(length) {
  let result = '';
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  for (let i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * characters.length));
  }
  return result;
}

export function setup() {
  const geoRes = http.get(`${BASE_URL}/parking-lots/geojson`);
  let spots = [];
  
  if (geoRes.status === 200) {
    const lots = geoRes.json('features');
    if (lots && lots.length > 0) {
      const lotId = lots[0].properties.id;
      const spotsRes = http.get(`${BASE_URL}/parking-lots/${lotId}/spots`);
      if (spotsRes.status === 200) {
        spots = spotsRes.json();
      }
    }
  }

  let tokens = [];
  // Реєструємо 50 унікальних користувачів
  for (let i = 0; i < 50; i++) {
    const email = `k6user_${randomStr(6)}@parkflow.com`;
    const regRes = http.post(`${BASE_URL}/auth/register`, JSON.stringify({
      email: email,
      password: 'password123',
      fullName: 'Load Tester', // Обов'язкове поле згідно з RegisterRequest
    }), { headers: { 'Content-Type': 'application/json' } });
    
    // Auth controller повертає 200 OK з AuthResponse
    if (regRes.status === 200) {
      tokens.push(regRes.json('token'));
    }
  }

  return { spots, tokens };
}

export default function (data) {
  const { spots, tokens } = data;

  if (!spots || spots.length === 0 || !tokens || tokens.length === 0) {
    return;
  }

  // Вибираємо випадкового користувача
  const token = tokens[Math.floor(Math.random() * tokens.length)];
  
  // Вибираємо випадкове паркомісце
  const randomSpot = spots[Math.floor(Math.random() * spots.length)];
  if (!randomSpot || !randomSpot.parkingLotId) return;

  const now = new Date();
  const to = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  
  let availRes = http.get(`${BASE_URL}/parking-lots/${randomSpot.parkingLotId}/availability?from=${now.toISOString()}&to=${to.toISOString()}`);
  check(availRes, { 'Availability loaded': (r) => r.status === 200 });

  sleep(Math.random() * 2);

  if (Math.random() < 0.4) {
    const fromTime = new Date(now.getTime() + (Math.floor(Math.random() * 5) + 1) * 60 * 60 * 1000);
    const toTime = new Date(fromTime.getTime() + 2 * 60 * 60 * 1000);
    const licensePlate = `K6${randomStr(4).toUpperCase()}`;

    const payload = JSON.stringify({
      spotId: randomSpot.id,
      from: fromTime.toISOString(),
      to: toTime.toISOString(),
      licensePlate: licensePlate
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Idempotency-Key': `k6-${randomStr(10)}`
      }
    };

    let bookRes = http.post(`${BASE_URL}/reservations`, payload, params);
    
    check(bookRes, {
      'Booking success (201) or conflict (409)': (r) => r.status === 201 || r.status === 409
    });
  }

  sleep(Math.random() * 2 + 1);
}
