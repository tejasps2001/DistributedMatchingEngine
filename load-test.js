import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 1000,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 75
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
    const initialBalance = 100_000_000.00;
    const initialShares = 10_000_000;

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

export default function loadTest (data) {
    // Randomly select one of the users
    const randomNo = Math.floor(Math.random() * data.users.length)
    const userId = data.users[randomNo];

    // Segregate the users into buyers and sellers to prevent triggering
    // STP.
    const isBuy = randomNo <= 9;

    const endpoint = isBuy ? `${BASE_URL}/buy` : `${BASE_URL}/sell`;
    // Price range: 100.00 to 109.00
    const price = (100 + Math.floor(Math.random() * 10))
        .toFixed(2);
    // Quantity range: 1 to 5
    const quantity = Math.floor(Math.random() * 5) + 1;
    const side = isBuy ? 'BUY' : 'SELL';

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