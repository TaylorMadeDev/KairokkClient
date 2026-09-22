import { randomUUID } from 'node:crypto';
import { once } from 'node:events';
import WebSocket from 'ws';
import { describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { MemoryStore, testEnv } from './helpers.js';

const envelope = (type: string, payload: unknown) => ({ type, requestId: randomUUID(), timestamp: new Date().toISOString(), payload });

describe('world chunk delivery', () => {
  it('acknowledges only chunks accepted by the server', async () => {
    const store = new MemoryStore();
    const app = await buildApp(testEnv, store);
    const address = await app.listen({ host: '127.0.0.1', port: 0 });
    let socket: WebSocket | undefined;
    try {
      const registered = await app.inject({ method: 'POST', url: '/auth/register', payload: { username: 'world_user', email: 'world@example.com', password: 'secure-password-123' } });
      const accessToken = registered.json().accessToken as string;
      const fingerprint = 'a'.repeat(64);
      const device = await app.inject({ method: 'POST', url: '/devices/register', headers: { authorization: `Bearer ${accessToken}` }, payload: { deviceFingerprintHash: fingerprint, deviceName: 'Test client', platform: 'Windows' } });
      socket = new WebSocket(`${address.replace(/^http/, 'ws')}/client/connect?deviceFingerprintHash=${fingerprint}`, { headers: { authorization: `Bearer ${accessToken}` } });
      await once(socket, 'open');
      socket.send(JSON.stringify(envelope('HELLO', { protocolVersion: 1, clientVersion: 'test', minecraftVersion: '26.1.2', deviceId: device.json().device.id, capabilities: [] })));
      expect(JSON.parse(String((await once(socket, 'message'))[0])).type).toBe('HELLO_ACK');

      const accepted = envelope('EVENT', { event: 'WORLD_CHUNK', data: { chunk: { key: '0,0', x: 0, z: 0, revision: 42, blocks: [{ x: 0, y: 64, z: 0, kind: 'grass' }] } } });
      socket.send(JSON.stringify(accepted));
      expect(JSON.parse(String((await once(socket, 'message'))[0]))).toMatchObject({ type: 'WORLD_CHUNK_ACK', requestId: accepted.requestId, payload: { key: '0,0', revision: 42 } });

      socket.send(JSON.stringify(envelope('EVENT', { event: 'WORLD_CHUNK', data: { chunk: { key: 'bad', x: 0, z: 0, revision: -1, blocks: [] } } })));
      expect(JSON.parse(String((await once(socket, 'message'))[0])).type).toBe('ERROR');
    } finally {
      socket?.close();
      await app.close();
    }
  });
});
