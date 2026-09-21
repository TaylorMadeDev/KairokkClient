import { afterEach, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { MemoryStore, testEnv } from './helpers.js';

const password = 'correct-horse-battery-staple';
const registration = { username: 'kairokk_user', email: 'user@example.com', password };
const apps: Awaited<ReturnType<typeof buildApp>>[] = [];
const create = async () => { const app = await buildApp(testEnv, new MemoryStore()); apps.push(app); return app; };
afterEach(async () => { await Promise.all(apps.splice(0).map((app) => app.close())); });
describe('authentication', () => {
  it('registers and protects private data', async () => { const app = await create(); const response = await app.inject({ method: 'POST', url: '/auth/register', payload: registration }); expect(response.statusCode).toBe(201); expect(response.json().user.email).toBe(registration.email); expect(response.json().accessToken).toBeTypeOf('string'); });
  it('rejects duplicate usernames and emails', async () => { const app = await create(); await app.inject({ method: 'POST', url: '/auth/register', payload: registration }); expect((await app.inject({ method: 'POST', url: '/auth/register', payload: { ...registration, email: 'new@example.com' } })).json().error.code).toBe('USERNAME_TAKEN'); expect((await app.inject({ method: 'POST', url: '/auth/register', payload: { ...registration, username: 'new_user' } })).json().error.code).toBe('EMAIL_TAKEN'); });
  it('logs in and rejects incorrect passwords', async () => { const app = await create(); await app.inject({ method: 'POST', url: '/auth/register', payload: registration }); expect((await app.inject({ method: 'POST', url: '/auth/login', payload: { email: registration.email, password } })).statusCode).toBe(200); expect((await app.inject({ method: 'POST', url: '/auth/login', payload: { email: registration.email, password: 'wrong-password' } })).json().error.code).toBe('INVALID_CREDENTIALS'); });
  it('rotates refresh tokens and revokes them on logout', async () => { const app = await create(); const register = await app.inject({ method: 'POST', url: '/auth/register', payload: registration }); const token = register.json().refreshToken; const refreshed = await app.inject({ method: 'POST', url: '/auth/refresh', payload: { refreshToken: token } }); expect(refreshed.statusCode).toBe(200); expect(refreshed.json().refreshToken).not.toBe(token); expect((await app.inject({ method: 'POST', url: '/auth/refresh', payload: { refreshToken: token } })).json().error.code).toBe('INVALID_REFRESH_TOKEN'); await app.inject({ method: 'POST', url: '/auth/logout', payload: { refreshToken: refreshed.json().refreshToken } }); expect((await app.inject({ method: 'POST', url: '/auth/refresh', payload: { refreshToken: refreshed.json().refreshToken } })).statusCode).toBe(401); });
  it('requires access tokens for protected routes', async () => { const app = await create(); expect((await app.inject({ method: 'GET', url: '/auth/me' })).json().error.code).toBe('UNAUTHORIZED'); });
});
describe('devices', () => {
  it('registers a first device and enforces the single-device limit', async () => { const app = await create(); const register = await app.inject({ method: 'POST', url: '/auth/register', payload: registration }); const auth = { authorization: `Bearer ${register.json().accessToken}` }; const one = 'a'.repeat(64); const two = 'b'.repeat(64); expect((await app.inject({ method: 'POST', url: '/devices/register', headers: auth, payload: { deviceFingerprintHash: one, deviceName: 'Main PC', platform: 'Windows' } })).statusCode).toBe(201); const rejected = await app.inject({ method: 'POST', url: '/devices/register', headers: auth, payload: { deviceFingerprintHash: two, deviceName: 'Second PC', platform: 'Windows' } }); expect(rejected.statusCode).toBe(403); expect(rejected.json().error.code).toBe('DEVICE_LIMIT_REACHED'); });
});
describe('notifications', () => {
  it('requires authentication and never exposes another account notification state', async () => {
    const app = await create();
    expect((await app.inject({ method: 'GET', url: '/api/notifications' })).statusCode).toBe(401);
    const first = await app.inject({ method: 'POST', url: '/auth/register', payload: registration });
    const second = await app.inject({ method: 'POST', url: '/auth/register', payload: { ...registration, username: 'other_user', email: 'other@example.com' } });
    const response = await app.inject({ method: 'POST', url: '/api/notifications/00000000-0000-0000-0000-000000000001/read', headers: { authorization: `Bearer ${second.json().accessToken}` } });
    expect(response.statusCode).toBe(404);
    expect((await app.inject({ method: 'GET', url: '/api/notifications', headers: { authorization: `Bearer ${first.json().accessToken}` } })).statusCode).toBe(200);
  });
});
