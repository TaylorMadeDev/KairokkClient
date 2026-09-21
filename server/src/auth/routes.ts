import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { z } from 'zod';
import { AppError } from '../lib/errors.js';
import type { AuthService } from './service.js';

const credentials = z.object({ username: z.string().trim().min(3).max(32).regex(/^[a-zA-Z0-9_]+$/), email: z.string().trim().email().max(320).transform((value) => value.toLowerCase()), password: z.string().min(12).max(128) });
const login = z.object({ email: z.string().trim().min(3).max(320), password: z.string().min(1).max(128) });
const refresh = z.object({ refreshToken: z.string().min(32).optional() });
const refreshCookie = 'kairokk_refresh';
const requestInfo = (request: FastifyRequest) => ({ ip: request.ip, userAgent: request.headers['user-agent'] });

export const authRoutes = async (app: FastifyInstance, auth: AuthService) => {
  const setRefresh = (reply: FastifyReply, value: string) => reply.setCookie(refreshCookie, value, { httpOnly: true, secure: app.kairokk.env.COOKIE_SECURE, sameSite: 'strict', path: '/auth', maxAge: 60 * 60 * 24 * 30 });
  app.post('/auth/register', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
    const input = credentials.parse(request.body); const user = await auth.register(input.username, input.email, input.password); const tokens = await auth.issueTokens(user, requestInfo(request)); setRefresh(reply, tokens.refreshToken);
    return reply.code(201).send({ user: publicUser(user), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
  });
  app.post('/auth/login', { config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    const input = login.parse(request.body); const user = await auth.login(input.email, input.password); const tokens = await auth.issueTokens(user, requestInfo(request)); setRefresh(reply, tokens.refreshToken);
    return { user: publicUser(user), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
  });
  app.post('/auth/refresh', { config: { rateLimit: { max: 20, timeWindow: '1 minute' } } }, async (request, reply) => {
    const input = refresh.parse(request.body ?? {}); const token = input.refreshToken ?? request.cookies[refreshCookie]; if (!token) throw new AppError('INVALID_REFRESH_TOKEN', 401, 'Refresh token is required.'); const tokens = await auth.rotate(token, requestInfo(request)); setRefresh(reply, tokens.refreshToken); return { accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
  });
  app.post('/auth/logout', async (request, reply) => { const input = refresh.parse(request.body ?? {}); await auth.logout(input.refreshToken ?? request.cookies[refreshCookie]); reply.clearCookie(refreshCookie, { path: '/auth' }); return reply.code(204).send(); });
  app.get('/auth/me', { preHandler: app.authenticate }, async (request) => { const user = await app.kairokk.store.findUserById(request.userId); if (!user || user.isDisabled) throw new AppError('UNAUTHORIZED', 401, 'A valid access token is required.'); return { user: publicUser(user) }; });
};
const publicUser = (user: { id: string; username: string; email: string; role: string; createdAt: Date }) => ({ id: user.id, username: user.username, email: user.email, role: user.role, createdAt: user.createdAt });
