import argon2 from 'argon2';
import { SignJWT, jwtVerify } from 'jose';
import { AppError } from '../lib/errors.js';
import { hashRefreshToken, opaqueToken } from '../lib/crypto.js';
import type { Env } from '../lib/env.js';
import type { Store } from '../lib/store.js';
import type { User } from '../types/domain.js';

export class AuthService {
  private readonly jwtKey: Uint8Array;
  constructor(private readonly store: Store, private readonly env: Env) { this.jwtKey = new TextEncoder().encode(env.JWT_SECRET); }
  async register(username: string, email: string, password: string) {
    if (await this.store.findUserByUsername(username)) throw new AppError('USERNAME_TAKEN', 409, 'Username is already in use.');
    if (await this.store.findUserByEmail(email)) throw new AppError('EMAIL_TAKEN', 409, 'Email is already in use.');
    const passwordHash = await argon2.hash(password, { type: argon2.argon2id, memoryCost: 19456, timeCost: 2, parallelism: 1 });
    const user = await this.store.createUser({ username, email, passwordHash });
    await this.store.activity(user.id, 'USER_REGISTERED');
    return user;
  }
  async login(identity: string, password: string) {
    const normalizedIdentity = identity.trim();
    const user = (await this.store.findUserByEmail(normalizedIdentity.toLowerCase())) ?? await this.store.findUserByUsername(normalizedIdentity);
    if (!user || !(await argon2.verify(user.passwordHash, password))) throw new AppError('INVALID_CREDENTIALS', 401, 'Email or password is incorrect.');
    if (user.isDisabled) throw new AppError('ACCOUNT_DISABLED', 403, 'This account is disabled.');
    await this.store.touchUserLogin(user.id); await this.store.activity(user.id, 'USER_LOGGED_IN');
    return user;
  }
  async issueTokens(user: User, request: { ip?: string; userAgent?: string }) {
    void request;
    const accessToken = await new SignJWT({ username: user.username, role: user.role }).setProtectedHeader({ alg: 'HS256' }).setSubject(user.id).setIssuedAt().setExpirationTime('15m').sign(this.jwtKey);
    const refreshToken = opaqueToken();
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await this.store.createSession({ userId: user.id, refreshTokenHash: hashRefreshToken(refreshToken, this.env.REFRESH_TOKEN_PEPPER), createdAt: new Date(), expiresAt });
    return { accessToken, refreshToken, refreshExpiresAt: expiresAt };
  }
  async rotate(refreshToken: string, request: { ip?: string; userAgent?: string }) {
    const session = await this.store.findSessionByHash(hashRefreshToken(refreshToken, this.env.REFRESH_TOKEN_PEPPER));
    if (!session || session.revokedAt || session.expiresAt <= new Date()) throw new AppError('INVALID_REFRESH_TOKEN', 401, 'Refresh token is invalid or expired.');
    const user = await this.store.findUserById(session.userId);
    if (!user || user.isDisabled) throw new AppError('INVALID_REFRESH_TOKEN', 401, 'Refresh token is invalid or expired.');
    await this.store.revokeSession(session.id); return this.issueTokens(user, request);
  }
  async logout(refreshToken?: string) { if (refreshToken) { const session = await this.store.findSessionByHash(hashRefreshToken(refreshToken, this.env.REFRESH_TOKEN_PEPPER)); if (session) await this.store.revokeSession(session.id); } }
  refreshHash(refreshToken: string) { return hashRefreshToken(refreshToken, this.env.REFRESH_TOKEN_PEPPER); }
  async verifyAccess(token: string) {
    try { const { payload } = await jwtVerify(token, this.jwtKey); if (!payload.sub) throw new Error('missing sub'); return payload.sub; }
    catch { throw new AppError('UNAUTHORIZED', 401, 'A valid access token is required.'); }
  }
}
