import { randomUUID } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import type { Device, Session, User } from '../types/domain.js';
import type { ConfigData, DashboardData, ScriptData, Store } from './store.js';

type EventData = { id: string; userId: string; type: string; metadata: Record<string, unknown> | null; createdAt: Date };
type ClientData = { id: string; userId: string; deviceId: string; clientVersion: string; minecraftVersion: string; serverAddress: string | null; lastHeartbeatAt: Date; status: 'CONNECTED' | 'DISCONNECTED' };
type LocalData = {
  users: User[];
  sessions: Session[];
  devices: Device[];
  events: EventData[];
  configs: (ConfigData & { userId: string })[];
  scripts: (ScriptData & { userId: string })[];
  clients: ClientData[];
};

const emptyData = (): LocalData => ({ users: [], sessions: [], devices: [], events: [], configs: [], scripts: [], clients: [] });
const asDate = (value: unknown) => new Date(value as string);

export class LocalStore implements Store {
  private data: LocalData = emptyData();
  private ready: Promise<void>;
  private writes = Promise.resolve();

  constructor(private readonly file: string) { this.ready = this.load(); }

  private async load() {
    try {
      const parsed = JSON.parse(await readFile(this.file, 'utf8')) as LocalData;
      this.data = parsed;
      for (const user of this.data.users) { user.createdAt = asDate(user.createdAt); user.updatedAt = asDate(user.updatedAt); user.lastLoginAt = user.lastLoginAt ? asDate(user.lastLoginAt) : null; }
      for (const session of this.data.sessions) { session.createdAt = asDate(session.createdAt); session.expiresAt = asDate(session.expiresAt); session.revokedAt = session.revokedAt ? asDate(session.revokedAt) : null; }
      for (const device of this.data.devices) { device.firstSeenAt = asDate(device.firstSeenAt); device.lastSeenAt = asDate(device.lastSeenAt); }
      for (const event of this.data.events) event.createdAt = asDate(event.createdAt);
      for (const config of this.data.configs) { config.createdAt = asDate(config.createdAt); config.updatedAt = asDate(config.updatedAt); }
      for (const script of this.data.scripts) { script.createdAt = asDate(script.createdAt); script.updatedAt = asDate(script.updatedAt); }
      for (const client of this.data.clients) client.lastHeartbeatAt = asDate(client.lastHeartbeatAt);
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
      await this.persist();
    }
  }

  private persist() {
    this.writes = this.writes.then(async () => { await mkdir(dirname(this.file), { recursive: true }); await writeFile(this.file, JSON.stringify(this.data, null, 2), 'utf8'); });
    return this.writes;
  }

  private async loaded() { await this.ready; }
  async ensureDefaultAdmin(passwordHash: string) {
    await this.loaded();
    if (this.data.users.some((user) => user.username.toLowerCase() === 'admin')) return;
    const now = new Date();
    this.data.users.push({ id: randomUUID(), username: 'admin', email: 'admin@kairokk.local', passwordHash, role: 'ADMIN', isDisabled: false, createdAt: now, updatedAt: now, lastLoginAt: null });
    await this.persist();
  }
  async findUserByEmail(email: string) { await this.loaded(); return this.data.users.find((user) => user.email === email.toLowerCase()) ?? null; }
  async findUserByUsername(username: string) { await this.loaded(); return this.data.users.find((user) => user.username.toLowerCase() === username.toLowerCase()) ?? null; }
  async findUserById(id: string) { await this.loaded(); return this.data.users.find((user) => user.id === id) ?? null; }
  async createUser(input: Pick<User, 'username' | 'email' | 'passwordHash'>) { await this.loaded(); const now = new Date(); const user: User = { id: randomUUID(), ...input, email: input.email.toLowerCase(), role: 'USER', isDisabled: false, createdAt: now, updatedAt: now, lastLoginAt: null }; this.data.users.push(user); await this.persist(); return user; }
  async touchUserLogin(id: string) { const user = await this.findUserById(id); if (user) { user.lastLoginAt = new Date(); await this.persist(); } }
  async createSession(input: Omit<Session, 'id' | 'revokedAt'>) { await this.loaded(); const session: Session = { id: randomUUID(), ...input, revokedAt: null }; this.data.sessions.push(session); await this.persist(); return session; }
  async findSessionByHash(hash: string) { await this.loaded(); return this.data.sessions.find((session) => session.refreshTokenHash === hash) ?? null; }
  async revokeSession(id: string) { await this.loaded(); const session = this.data.sessions.find((entry) => entry.id === id); if (session) { session.revokedAt = new Date(); await this.persist(); } }
  async findDevice(userId: string, fingerprint: string) { await this.loaded(); return this.data.devices.find((device) => device.userId === userId && device.deviceFingerprintHash === fingerprint) ?? null; }
  async listDevices(userId: string) { await this.loaded(); return this.data.devices.filter((device) => device.userId === userId); }
  async createDevice(input: Pick<Device, 'userId' | 'deviceFingerprintHash' | 'deviceName' | 'platform'>) { await this.loaded(); const now = new Date(); const device: Device = { id: randomUUID(), ...input, firstSeenAt: now, lastSeenAt: now, isAuthorized: true }; this.data.devices.push(device); await this.persist(); return device; }
  async touchDevice(id: string) { await this.loaded(); const device = this.data.devices.find((entry) => entry.id === id); if (device) { device.lastSeenAt = new Date(); await this.persist(); } }
  async deleteDevice(userId: string, id: string) { await this.loaded(); const index = this.data.devices.findIndex((device) => device.id === id && device.userId === userId); if (index < 0) return false; this.data.devices.splice(index, 1); await this.persist(); return true; }
  async activity(userId: string, type: string, metadata?: Record<string, unknown>) { await this.loaded(); this.data.events.push({ id: randomUUID(), userId, type, metadata: metadata ?? null, createdAt: new Date() }); await this.persist(); }
  async dashboard(userId: string): Promise<DashboardData> { await this.loaded(); const client = this.data.clients.filter((entry) => entry.userId === userId).sort((a, b) => b.lastHeartbeatAt.getTime() - a.lastHeartbeatAt.getTime())[0]; return { statistics: { playTime: '0', sessions: 0, blocksTravelled: 0, macrosUsed: 0 }, configs: this.data.configs.filter((entry) => entry.userId === userId).length, scripts: this.data.scripts.filter((entry) => entry.userId === userId).length, activity: this.data.events.filter((entry) => entry.userId === userId).sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime()).slice(0, 10), client: { status: client?.status === 'CONNECTED' && client.lastHeartbeatAt > new Date(Date.now() - 45_000) ? 'ONLINE' : 'OFFLINE', version: client?.clientVersion ?? null, serverAddress: client?.serverAddress ?? null } }; }
  async listConfigs(userId: string) { await this.loaded(); return this.data.configs.filter((entry) => entry.userId === userId).sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime()); }
  async saveConfig(userId: string, name: string, data: unknown) { await this.loaded(); const existing = this.data.configs.find((entry) => entry.userId === userId && entry.name === name); if (existing) { existing.data = data; existing.updatedAt = new Date(); await this.persist(); return existing; } const now = new Date(); const config = { id: randomUUID(), userId, name, data, createdAt: now, updatedAt: now }; this.data.configs.push(config); await this.persist(); return config; }
  async removeConfig(userId: string, id: string) { await this.loaded(); const index = this.data.configs.findIndex((entry) => entry.userId === userId && entry.id === id); if (index < 0) return false; this.data.configs.splice(index, 1); await this.persist(); return true; }
  async listScripts(userId: string) { await this.loaded(); return this.data.scripts.filter((entry) => entry.userId === userId).sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime()); }
  async saveScript(userId: string, input: { id?: string; name: string; content: string; enabled: boolean }) { await this.loaded(); const existing = input.id ? this.data.scripts.find((entry) => entry.userId === userId && entry.id === input.id) : undefined; if (existing) { Object.assign(existing, input, { updatedAt: new Date() }); await this.persist(); return existing; } const now = new Date(); const script = { id: randomUUID(), userId, name: input.name, content: input.content, enabled: input.enabled, createdAt: now, updatedAt: now }; this.data.scripts.push(script); await this.persist(); return script; }
  async removeScript(userId: string, id: string) { await this.loaded(); const index = this.data.scripts.findIndex((entry) => entry.userId === userId && entry.id === id); if (index < 0) return false; this.data.scripts.splice(index, 1); await this.persist(); return true; }
  async updateUser(userId: string, input: { username?: string; passwordHash?: string }) { const user = await this.findUserById(userId); if (!user) throw new Error('User not found'); Object.assign(user, input, { updatedAt: new Date() }); await this.persist(); return user; }
  async listActiveSessions(userId: string) { await this.loaded(); return this.data.sessions.filter((entry) => entry.userId === userId && !entry.revokedAt && entry.expiresAt > new Date()).map(({ id, createdAt, expiresAt }) => ({ id, createdAt, expiresAt })); }
  async revokeOtherSessions(userId: string, currentRefreshHash?: string) { await this.loaded(); for (const session of this.data.sessions) if (session.userId === userId && !session.revokedAt && session.refreshTokenHash !== currentRefreshHash) session.revokedAt = new Date(); await this.persist(); }
  async connectClient(input: { userId: string; deviceId: string; clientVersion: string; minecraftVersion: string }) { await this.loaded(); const id = randomUUID(); this.data.clients.push({ id, ...input, serverAddress: null, lastHeartbeatAt: new Date(), status: 'CONNECTED' }); await this.persist(); return id; }
  async heartbeatClient(id: string, data: { server?: string | null }) { await this.loaded(); const client = this.data.clients.find((entry) => entry.id === id); if (client) { client.lastHeartbeatAt = new Date(); client.serverAddress = data.server ?? null; await this.persist(); } }
  async disconnectClient(id: string) { await this.loaded(); const client = this.data.clients.find((entry) => entry.id === id); if (client) { client.status = 'DISCONNECTED'; await this.persist(); } }
  async listNotifications() { return []; }
  async markNotification() { return false; }
  async markAllNotifications() {}
  async disconnect() { await this.writes; }
}
