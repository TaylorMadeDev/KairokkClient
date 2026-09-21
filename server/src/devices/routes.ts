import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { AppError } from '../lib/errors.js';

const registerSchema = z.object({
  deviceFingerprintHash: z.string().regex(/^[a-f0-9]{64}$/i, 'Must be a SHA-256 fingerprint hash.'),
  deviceName: z.string().trim().min(1).max(128),
  platform: z.string().trim().min(1).max(64)
});

// FREE intentionally maps to one device; subscription policy can change this lookup later.
const deviceLimitForUser = () => 1;
export const deviceRoutes = async (app: FastifyInstance) => {
  app.post('/devices/register', { preHandler: app.authenticate }, async (request, reply) => {
    const input = registerSchema.parse(request.body); const existing = await app.kairokk.store.findDevice(request.userId, input.deviceFingerprintHash);
    if (existing) { if (!existing.isAuthorized) throw new AppError('DEVICE_NOT_AUTHORIZED', 403, 'This device is not authorized.'); await app.kairokk.store.touchDevice(existing.id); return { device: existing, alreadyRegistered: true }; }
    const devices = await app.kairokk.store.listDevices(request.userId);
    if (devices.filter((device) => device.isAuthorized).length >= deviceLimitForUser()) throw new AppError('DEVICE_LIMIT_REACHED', 403, 'This account has reached its authorized device limit.');
    const device = await app.kairokk.store.createDevice({ userId: request.userId, ...input }); await app.kairokk.store.activity(request.userId, 'DEVICE_REGISTERED', { deviceId: device.id, platform: device.platform });
    return reply.code(201).send({ device, alreadyRegistered: false });
  });
  app.get('/devices', { preHandler: app.authenticate }, async (request) => ({ devices: await app.kairokk.store.listDevices(request.userId) }));
  app.delete('/devices/:id', { preHandler: app.authenticate }, async (request, reply) => {
    const { id } = z.object({ id: z.string().uuid() }).parse(request.params); const removed = await app.kairokk.store.deleteDevice(request.userId, id); if (!removed) throw new AppError('DEVICE_NOT_FOUND', 404, 'Device was not found.'); await app.kairokk.store.activity(request.userId, 'DEVICE_REMOVED', { deviceId: id }); return reply.code(204).send();
  });
};
