import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 1000,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 50,      // Pre-allocated concurrent Tomcat client connections
            maxVUs: 200,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<50', 'p(99)<150'],
    },
};

const BASE_URL = 'http://localhost:8080';

// Create 20 users with huge balance and shares
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
            console.error(`Failed to create user #${i}: status ${res.status}`);
        }
    }

    if (users.length === 0) {
        throw new Error('Could not create any users.');
    }

    return { users: users };
}

// 2. VU Execution Loop
export default function (data) {
    // Randomly select one of the pre-funded users
    const userId = data.users[Math.floor(Math.random() * data.users.length)];
    const isBuy = Math.random() > 0.5;
export default function loadTest (data) {

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

    check(res, {
        'Order accepted or filled (200 or 202)': (r) => r.status === 200
            || r.status === 202
    });
}