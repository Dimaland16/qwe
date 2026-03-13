import http from 'k6/http';
import { check, sleep } from 'k6';

const LOOM_URL = 'http://host.docker.internal:8081/api/v1/currencies/USD/sync-and-compare';
const REACTOR_URL = 'http://host.docker.internal:8082/api/v1/currencies/USD/sync-and-compare';

export const options = {
    scenarios: {
        smoke_loom: { executor: 'constant-vus', vus: 5, duration: '10s', exec: 'hitLoom', tags: { app: 'loom' } },
        smoke_reactor: { executor: 'constant-vus', vus: 5, duration: '10s', exec: 'hitReactor', tags: { app: 'reactor' } },

        stress_loom: {
            executor: 'ramping-vus', startTime: '10s', startVUs: 0,
            stages:[
                { duration: '10s', target: 100 },
                { duration: '30s', target: 100 },
                { duration: '10s', target: 0 }
            ],
            exec: 'hitLoom', tags: { app: 'loom' }
        },
        stress_reactor: {
            executor: 'ramping-vus', startTime: '10s', startVUs: 0,
            stages:[
                { duration: '10s', target: 100 },
                { duration: '30s', target: 100 },
                { duration: '10s', target: 0 }
            ],
            exec: 'hitReactor', tags: { app: 'reactor' }
        },

        corruption_clash: {
            executor: 'constant-vus', startTime: '30s', vus: 50, duration: '15s', exec: 'hitBothForDeadlock', tags: { app: 'both' }
        }
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
    }
};

export function hitLoom() {
    let res = http.get(LOOM_URL);
    validateResponse(res, 'Loom');
    sleep(Math.random() * 0.5);
}

export function hitReactor() {
    let res = http.get(REACTOR_URL);
    validateResponse(res, 'Reactor');
    sleep(Math.random() * 0.5);
}

export function hitBothForDeadlock() {
    let responses = http.batch([
        ['GET', LOOM_URL],
        ['GET', REACTOR_URL]
    ]);
    validateResponse(responses[0], 'Loom_Clash');
    validateResponse(responses[1], 'Reactor_Clash');
}

function validateResponse(res, label) {
    check(res, {[`${label} status 200`]: (r) => r.status === 200,
        [`${label} valid delta`]: (r) => {
            if (r.status !== 200) return false;
            try {
                const body = JSON.parse(r.body);
                return body.delta !== undefined;
            } catch (e) {
                return false;
            }
        }
    });
}