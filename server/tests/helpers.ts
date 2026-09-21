import { randomUUID } from 'node:crypto';
import type { ConfigData, DashboardData, ScriptData, Store } from '../src/lib/store.js';
import type { Device, Session, User } from '../src/types/domain.js';

export class MemoryStore implements Store {
  users: User[] = []; sessions: Session[] = []; devices: Device[] = []; events: { userId: string; type: string }[] = []; configs: (ConfigData & { userId: string })[] = []; scripts: (ScriptData & { userId: string })[] = [];
  async findUserByEmail(email: string) { return this.users.find((user) => user.email === email) ?? null; }
  async findUserByUsername(username: string) { return this.users.find((user) => user.username === username) ?? null; }
  async findUserById(id: string) { return this.users.find((user) => user.id === id) ?? null; }
  async createUser(input: Pick<User, 'username' | 'email' | 'passwordHash'>) { const now = new Date(); const user: User = { id: randomUUID(), ...input, role: 'USER', isDisabled: false, createdAt: now, updatedAt: now, lastLoginAt: null }; this.users.push(user); return user; }
  async touchUserLogin(id: string) { const user = await this.findUserById(id); if (user) user.lastLoginAt = new Date(); }
  async createSession(input: Omit<Session, 'id' | 'revokedAt'>) { const session: Session = { id: randomUUID(), ...input, revokedAt: null }; this.sessions.push(session); return session; }
  async findSessionByHash(hash: string) { return this.sessions.find((session) => session.refreshTokenHash === hash) ?? null; }
  async revokeSession(id: string) { const session = this.sessions.find((entry) => entry.id === id); if (session) session.revokedAt = new Date(); }
  async findDevice(userId: string, fingerprint: string) { return this.devices.find((device) => device.userId === userId && device.deviceFingerprintHash === fingerprint) ?? null; }
  async listDevices(userId: string) { return this.devices.filter((device) => device.userId === userId); }
  async createDevice(input: Pick<Device, 'userId' | 'deviceFingerprintHash' | 'deviceName' | 'platform'>) { const now = new Date(); const device: Device = { id: randomUUID(), ...input, firstSeenAt: now, lastSeenAt: now, isAuthorized: true }; this.devices.push(device); return device; }
  async touchDevice(id: string) { const device = this.devices.find((entry) => entry.id === id); if (device) device.lastSeenAt = new Date(); }
  async deleteDevice(userId: string, id: string) { const index = this.devices.findIndex((device) => device.id === id && device.userId === userId); if (index < 0) return false; this.devices.splice(index, 1); return true; }
  async activity(userId: string, type: string) { this.events.push({ userId, type }); }
  async dashboard(userId: string): Promise<DashboardData> { return { statistics: { playTime: '0', sessions: 0, blocksTravelled: 0, macrosUsed: 0 }, configs: this.configs.filter((entry) => entry.userId === userId).length, scripts: this.scripts.filter((entry) => entry.userId === userId).length, activity: [], client: { status: 'OFFLINE', version: null, serverAddress: null } }; }
  async listConfigs(userId: string) { return this.configs.filter((entry) => entry.userId === userId); }
  async saveConfig(userId: string, name: string, data: unknown) { const existing = this.configs.find((entry) => entry.userId === userId && entry.name === name); if (existing) { existing.data = data; existing.updatedAt = new Date(); return existing; } const now = new Date(); const config = { id: randomUUID(), userId, name, data, createdAt: now, updatedAt: now }; this.configs.push(config); return config; }
  async removeConfig(userId: string, id: string) { const i = this.configs.findIndex((entry) => entry.userId === userId && entry.id === id); if (i < 0) return false; this.configs.splice(i, 1); return true; }
  async listScripts(userId: string) { return this.scripts.filter((entry) => entry.userId === userId); }
  async saveScript(userId: string, input: { id?: string; name: string; content: string; enabled: boolean }) { const existing = input.id && this.scripts.find((entry) => entry.userId === userId && entry.id === input.id); if (existing) { Object.assign(existing, input, { updatedAt: new Date() }); return existing; } const now = new Date(); const script = { id: randomUUID(), userId, name: input.name, content: input.content, enabled: input.enabled, createdAt: now, updatedAt: now }; this.scripts.push(script); return script; }
  async removeScript(userId: string, id: string) { const i = this.scripts.findIndex((entry) => entry.userId === userId && entry.id === id); if (i < 0) return false; this.scripts.splice(i, 1); return true; }
  async updateUser(userId: string, input: { username?: string; passwordHash?: string }) { const user = await this.findUserById(userId); if (!user) throw new Error('User not found'); Object.assign(user, input, { updatedAt: new Date() }); return user; }
  async listActiveSessions(userId: string) { return this.sessions.filter((entry) => entry.userId === userId && !entry.revokedAt && entry.expiresAt > new Date()).map(({ id, createdAt, expiresAt }) => ({ id, createdAt: createdAt ?? new Date(), expiresAt })); }
  async revokeOtherSessions(userId: string, currentRefreshHash?: string) { for (const session of this.sessions) if (session.userId === userId && !session.revokedAt && session.refreshTokenHash !== currentRefreshHash) session.revokedAt = new Date(); }
  async connectClient() { return randomUUID(); }
  async heartbeatClient() {}
  async disconnectClient() {}
  async listNotifications() { return []; }
  async markNotification() { return false; }
  async markAllNotifications() {}
}
export const testEnv = { STORAGE_MODE: 'local' as const, KAIROKK_DATA_FILE: '.data/test.json', DATABASE_URL: 'postgresql://test:test@localhost:5432/test', JWT_SECRET: 'a'.repeat(32), REFRESH_TOKEN_PEPPER: 'b'.repeat(32), CORS_ORIGIN: 'http://localhost:5173', PORT: 3000, HOST: '127.0.0.1', NODE_ENV: 'test' as const, COOKIE_SECURE: false };
