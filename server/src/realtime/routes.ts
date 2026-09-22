import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { clientMessage } from './protocol.js';

const query = z.object({ deviceFingerprintHash: z.string().regex(/^[a-f0-9]{64}$/i) });
const browserQuery = z.object({ accessToken: z.string().min(20) });
const browserMessage = z.discriminatedUnion('type', [z.object({ type: z.literal('WORLD_SUBSCRIBE'), radius: z.number().int().min(1).max(8).default(4) }), z.object({ type: z.literal('WORLD_UNSUBSCRIBE') }), z.object({ type: z.literal('WORLD_RESYNC_REQUEST') })]);
export const realtimeRoutes = async (app: FastifyInstance) => {
  app.get('/client/connect', { websocket: true }, async (socket, request) => {
    const input = query.parse(request.query); await app.authenticate(request); const device = await app.kairokk.store.findDevice(request.userId, input.deviceFingerprintHash);
    if (!device?.isAuthorized) { socket.close(4003, 'DEVICE_NOT_AUTHORIZED'); return; }
    let hello = false; let instanceId: string | undefined; const connectedAt = Date.now(); const watchdog = setTimeout(() => socket.close(4008, 'HELLO_REQUIRED'), 10_000);
    socket.on('message', async (payload: Buffer) => {
      try {
        const message = clientMessage.parse(JSON.parse(payload.toString()));
        if (!hello) {
          if (message.type !== 'HELLO' || message.payload.deviceId !== device.id) { socket.close(4003, 'INVALID_HELLO'); return; }
          hello = true; clearTimeout(watchdog);
          instanceId = await app.kairokk.store.connectClient({ userId: request.userId, deviceId: device.id, clientVersion: message.payload.clientVersion, minecraftVersion: message.payload.minecraftVersion });
          await app.kairokk.store.touchDevice(device.id);
          await app.kairokk.store.activity(request.userId, 'CLIENT_CONNECTED', { deviceId: device.id, clientVersion: message.payload.clientVersion });
          app.kairokk.hub.connect({ userId: request.userId, deviceId: device.id, socket, capabilities: message.payload.capabilities });
          socket.send(JSON.stringify({ type: 'HELLO_ACK', requestId: message.requestId, timestamp: new Date().toISOString(), payload: { heartbeatIntervalMs: 15_000 } }));
          if (app.kairokk.world.hasSubscribers(request.userId)) void app.kairokk.hub.command(request.userId, crypto.randomUUID(), 'WORLD_SUBSCRIBE', { radius: 4 }).catch(() => undefined);
          return;
        }
        if (message.type === 'HEARTBEAT') {
          await app.kairokk.store.touchDevice(device.id);
          if (instanceId) await app.kairokk.store.heartbeatClient(instanceId, { server: message.payload.server, latencyMs: Math.max(0, Date.now() - connectedAt) });
          socket.send(JSON.stringify({ type: 'HEARTBEAT_ACK', requestId: message.requestId, timestamp: new Date().toISOString(), payload: {} }));
        } else if (message.type === 'COMMAND_RESULT') app.kairokk.hub.result(message.requestId, message.payload);
        else if (message.type === 'EVENT' && (message.payload.event.startsWith('WORLD_') || message.payload.event === 'PATHFINDER_PATH')) {
          app.kairokk.world.receive(request.userId, message.payload);
          if (message.payload.event === 'WORLD_CHUNK') {
            const chunk = message.payload.data as { chunk: { key: string; revision: number } };
            socket.send(JSON.stringify({ type: 'WORLD_CHUNK_ACK', requestId: message.requestId, timestamp: new Date().toISOString(), payload: { key: chunk.chunk.key, revision: chunk.chunk.revision } }));
          }
        }
      } catch {
        socket.send(JSON.stringify({ type: 'ERROR', requestId: crypto.randomUUID(), timestamp: new Date().toISOString(), payload: { code: 'INVALID_MESSAGE' } }));
      }
    });
    socket.on('close', async () => { clearTimeout(watchdog); if (app.kairokk.hub.disconnect(request.userId, socket)) app.kairokk.world.clear(request.userId); if (instanceId) await app.kairokk.store.disconnectClient(instanceId); await app.kairokk.store.activity(request.userId, 'CLIENT_DISCONNECTED', { deviceId: device.id }); });
  });
  app.get('/world/connect', { websocket: true }, async (socket, request) => {
    try { const { accessToken } = browserQuery.parse(request.query); const userId = await app.kairokk.auth.verifyAccess(accessToken); let subscribed = false; socket.on('message', async (payload: Buffer) => { try { const message = browserMessage.parse(JSON.parse(payload.toString())); if (message.type === 'WORLD_SUBSCRIBE') { if (!subscribed) { subscribed = true; app.kairokk.world.subscribe(userId, socket); } await app.kairokk.hub.command(userId, crypto.randomUUID(), 'WORLD_SUBSCRIBE', { radius: message.radius }); } else if (message.type === 'WORLD_RESYNC_REQUEST') await app.kairokk.hub.command(userId, crypto.randomUUID(), 'WORLD_RESYNC_REQUEST', {}); else { if (subscribed) { subscribed = false; app.kairokk.world.unsubscribe(userId, socket); } await app.kairokk.hub.command(userId, crypto.randomUUID(), 'WORLD_UNSUBSCRIBE', {}); } } catch (error) { socket.send(JSON.stringify({ type: 'world:error', message: error instanceof Error ? error.message : 'World request failed.' })); } }); socket.on('close', () => { if (subscribed) { app.kairokk.world.unsubscribe(userId, socket); if (!app.kairokk.world.hasSubscribers(userId)) void app.kairokk.hub.command(userId, crypto.randomUUID(), 'WORLD_UNSUBSCRIBE', {}).catch(() => undefined); } }); }
    catch { socket.close(4003, 'UNAUTHORIZED'); }
  });
};
