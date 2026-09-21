import cookie from '@fastify/cookie';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import websocket from '@fastify/websocket';
import Fastify from 'fastify';
import { ZodError } from 'zod';
import { authRoutes } from './auth/routes.js';
import { apiRoutes } from './api/routes.js';
import { minecraftRoutes } from './api/minecraft.js';
import { AuthService } from './auth/service.js';
import { deviceRoutes } from './devices/routes.js';
import { errorBody, AppError } from './lib/errors.js';
import type { Env } from './lib/env.js';
import type { Store } from './lib/store.js';
import { realtimeRoutes } from './realtime/routes.js';
import { RealtimeHub } from './realtime/hub.js';
import { StreamManager } from './realtime/streams.js';
import { WorldViewHub } from './realtime/world.js';

declare module 'fastify' {
  interface FastifyInstance { authenticate: (request: FastifyRequest) => Promise<void>; kairokk: { env: Env; store: Store; auth: AuthService; hub: RealtimeHub; streams: StreamManager; world: WorldViewHub }; }
  interface FastifyRequest { userId: string; }
}
export const buildApp = async (env: Env, store: Store) => {
  const app = Fastify({ logger: env.NODE_ENV !== 'test' }); const auth = new AuthService(store, env); app.decorate('kairokk', { env, store, auth, hub: new RealtimeHub(), streams: new StreamManager(), world: new WorldViewHub() });
  await app.register(cookie); await app.register(cors, { origin: env.CORS_ORIGIN, credentials: true }); await app.register(rateLimit, { global: true, max: 100, timeWindow: '1 minute' }); await app.register(websocket);
  app.decorate('authenticate', async (request) => { const header = request.headers.authorization; if (!header?.startsWith('Bearer ')) throw new AppError('UNAUTHORIZED', 401, 'A valid access token is required.'); request.userId = await auth.verifyAccess(header.slice(7)); });
  app.setErrorHandler((error, request, reply) => { if (error instanceof ZodError) return reply.code(400).send({ error: { code: 'VALIDATION_ERROR', message: 'Request validation failed.', details: error.flatten() } }); const candidate = error as { statusCode?: unknown; code?: string; message?: string }; const status = error instanceof AppError ? error.statusCode : (typeof candidate.statusCode === 'number' ? candidate.statusCode : 500); if (status >= 500) request.log.error({ err: error }, 'Unhandled request error'); return reply.code(status).send(status >= 500 ? errorBody(error) : { error: { code: candidate.code ?? 'BAD_REQUEST', message: candidate.message ?? 'Request failed.' } }); });
  app.get('/health', async () => ({ status: 'ok' })); await app.register(authRoutes, auth); await app.register(apiRoutes); await app.register(minecraftRoutes); await app.register(deviceRoutes); await app.register(realtimeRoutes); return app;
};
