import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 1000,              // Target: 1,000 HTTP requests per second
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 50,      // Pre-allocated concurrent Tomcat client connections
            maxVUs: 200,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],               // Less than 1% errors
        http_req_duration: ['p(95)<50', 'p(99)<150'], // 95% under 50ms, 99% under 150ms
    },
};

const BASE_URL = 'http://localhost:8080';

// 1. Setup hook: Runs once before load generation starts
// Pre-creates 20 users with ample balance and shares to prevent 404 / 422 errors
export function setup() {
    const users = [];
    const initialBalance = 100000000.00;
    const initialShares = 10000000;

    for (let i = 0; i < 20; i++) {
        const url = `${BASE_URL}/createUserWithShares?balance=${initialBalance}&shares=${initialShares}`;
        const res = http.post(url);

        if (res.status === 200) {
            users.push(res.body.trim());
        } else {
            console.error(`Failed to pre-create user #${i}: status ${res.status}`);
        }
    }

    if (users.length === 0) {
        throw new Error('Could not pre-create any users. Is your Spring Boot app running on :8080?');
    }

    return { users: users };
}

// 2. VU Execution Loop
export default function (data) {
    // Randomly select one of the pre-funded users
    const userId = data.users[Math.floor(Math.random() * data.users.length)];
    const isBuy = Math.random() > 0.5;

    const endpoint = isBuy ? `${BASE_URL}/buy` : `${BASE_URL}/sell`;
    const price = (100 + Math.floor(Math.random() * 10)).toFixed(2);
    const side = isBuy ? 'BUY' : 'SELL';
    const quantity = Math.floor(Math.random() * 5) + 1;

    const payload = JSON.stringify({
        userId: userId,
        type: side,
        price: price,
        quantity: quantity,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post(endpoint, payload, params);

    // 3. Status checks:
    // - 200 OK: FILLED or PARTIALLY_FILLED
    // - 202 ACCEPTED: NEW (resting order added to book)
    check(res, {
        'order accepted or filled (200 or 202)': (r) => r.status === 200 || r.status === 202,
    });
}