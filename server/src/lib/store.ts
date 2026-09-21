import { Prisma, PrismaClient } from '@prisma/client';
import type { Device, Session, User } from '../types/domain.js';

export interface Store {
  findUserByEmail(email: string): Promise<User | null>;
  findUserByUsername(username: string): Promise<User | null>;
  findUserById(id: string): Promise<User | null>;
  createUser(input: Pick<User, 'username' | 'email' | 'passwordHash'>): Promise<User>;
  touchUserLogin(id: string): Promise<void>;
  createSession(input: Omit<Session, 'id' | 'revokedAt'>): Promise<Session>;
  findSessionByHash(hash: string): Promise<Session | null>;
  revokeSession(id: string): Promise<void>;
  findDevice(userId: string, fingerprint: string): Promise<Device | null>;
  listDevices(userId: string): Promise<Device[]>;
  createDevice(input: Pick<Device, 'userId' | 'deviceFingerprintHash' | 'deviceName' | 'platform'>): Promise<Device>;
  touchDevice(id: string): Promise<void>;
  deleteDevice(userId: string, id: string): Promise<boolean>;
  activity(userId: string, type: string, metadata?: Record<string, unknown>): Promise<void>;
  dashboard(userId: string): Promise<DashboardData>;
  listConfigs(userId: string): Promise<ConfigData[]>;
  saveConfig(userId: string, name: string, data: unknown): Promise<ConfigData>;
  removeConfig(userId: string, id: string): Promise<boolean>;
  listScripts(userId: string): Promise<ScriptData[]>;
  saveScript(userId: string, input: { id?: string; name: string; content: string; enabled: boolean }): Promise<ScriptData>;
  removeScript(userId: string, id: string): Promise<boolean>;
  updateUser(userId: string, input: { username?: string; passwordHash?: string }): Promise<User>;
  listActiveSessions(userId: string): Promise<{ id: string; createdAt: Date; expiresAt: Date }[]>;
  revokeOtherSessions(userId: string, currentRefreshHash?: string): Promise<void>;
  connectClient(input: { userId: string; deviceId: string; clientVersion: string; minecraftVersion: string }): Promise<string>;
  heartbeatClient(id: string, data: { server?: string | null; latencyMs?: number }): Promise<void>;
  disconnectClient(id: string): Promise<void>;
  listNotifications(userId: string, limit: number): Promise<{ id: string; type: string; title: string; body: string; readAt: Date | null; createdAt: Date }[]>;
  markNotification(userId: string, id: string): Promise<boolean>;
  markAllNotifications(userId: string): Promise<void>;
}
export type ConfigData = { id: string; name: string; data: unknown; createdAt: Date; updatedAt: Date };
export type ScriptData = { id: string; name: string; content: string; enabled: boolean; createdAt: Date; updatedAt: Date };
export type DashboardData = { statistics: { playTime: string; sessions: number; blocksTravelled: number; macrosUsed: number }; configs: number; scripts: number; activity: { id: string; type: string; metadata: unknown; createdAt: Date }[]; client: { status: 'ONLINE' | 'OFFLINE' | 'CONNECTING'; version: string | null; serverAddress: string | null } };

export class PrismaStore implements Store {
  constructor(private readonly db = new PrismaClient()) {}
  findUserByEmail(email: string) { return this.db.user.findUnique({ where: { email } }); }
  findUserByUsername(username: string) { return this.db.user.findUnique({ where: { username } }); }
  findUserById(id: string) { return this.db.user.findUnique({ where: { id } }); }
  createUser(input: Pick<User, 'username' | 'email' | 'passwordHash'>) { return this.db.user.create({ data: input }); }
  async touchUserLogin(id: string) { await this.db.user.update({ where: { id }, data: { lastLoginAt: new Date() } }); }
  createSession(input: Omit<Session, 'id' | 'revokedAt'>) { return this.db.session.create({ data: input }); }
  findSessionByHash(refreshTokenHash: string) { return this.db.session.findUnique({ where: { refreshTokenHash } }); }
  async revokeSession(id: string) { await this.db.session.update({ where: { id }, data: { revokedAt: new Date() } }); }
  findDevice(userId: string, deviceFingerprintHash: string) { return this.db.device.findUnique({ where: { userId_deviceFingerprintHash: { userId, deviceFingerprintHash } } }); }
  listDevices(userId: string) { return this.db.device.findMany({ where: { userId }, orderBy: { firstSeenAt: 'asc' } }); }
  createDevice(input: Pick<Device, 'userId' | 'deviceFingerprintHash' | 'deviceName' | 'platform'>) { return this.db.device.create({ data: input }); }
  async touchDevice(id: string) { await this.db.device.update({ where: { id }, data: { lastSeenAt: new Date() } }); }
  async deleteDevice(userId: string, id: string) { const result = await this.db.device.deleteMany({ where: { id, userId } }); return result.count === 1; }
  async activity(userId: string, type: string, metadata?: Record<string, unknown>) { await this.db.activityEvent.create({ data: { userId, type, metadata: metadata as Prisma.InputJsonValue | undefined } }); }
  async dashboard(userId: string): Promise<DashboardData> { const [statistics, configs, scripts, activity, client] = await Promise.all([this.db.playerStatistics.findUnique({ where: { userId } }), this.db.config.count({ where: { userId } }), this.db.script.count({ where: { userId } }), this.db.activityEvent.findMany({ where: { userId }, orderBy: { createdAt: 'desc' }, take: 10 }), this.db.clientInstance.findFirst({ where: { userId }, orderBy: { lastHeartbeatAt: 'desc' } })]); return { statistics: { playTime: (statistics?.playTime ?? 0n).toString(), sessions: statistics?.sessions ?? 0, blocksTravelled: statistics?.blocksTravelled ?? 0, macrosUsed: statistics?.macrosUsed ?? 0 }, configs, scripts, activity, client: { status: client?.status === 'CONNECTED' && client.lastHeartbeatAt > new Date(Date.now() - 45_000) ? 'ONLINE' : 'OFFLINE', version: client?.clientVersion ?? null, serverAddress: client?.serverAddress ?? null } }; }
  listConfigs(userId: string) { return this.db.config.findMany({ where: { userId }, orderBy: { updatedAt: 'desc' } }); }
  saveConfig(userId: string, name: string, data: unknown) { return this.db.config.upsert({ where: { userId_name: { userId, name } }, create: { userId, name, data: data as Prisma.InputJsonValue }, update: { data: data as Prisma.InputJsonValue } }); }
  async removeConfig(userId: string, id: string) { return (await this.db.config.deleteMany({ where: { id, userId } })).count === 1; }
  listScripts(userId: string) { return this.db.script.findMany({ where: { userId }, orderBy: { updatedAt: 'desc' } }); }
  async saveScript(userId: string, input: { id?: string; name: string; content: string; enabled: boolean }) { if (!input.id) return this.db.script.create({ data: { userId, name: input.name, content: input.content, enabled: input.enabled } }); const existing = await this.db.script.findFirst({ where: { id: input.id, userId } }); if (!existing) throw new Error('Script not found'); return this.db.script.update({ where: { id: input.id }, data: { name: input.name, content: input.content, enabled: input.enabled } }); }
  async removeScript(userId: string, id: string) { return (await this.db.script.deleteMany({ where: { id, userId } })).count === 1; }
  updateUser(id: string, input: { username?: string; passwordHash?: string }) { return this.db.user.update({ where: { id }, data: input }); }
  listActiveSessions(userId: string) { return this.db.session.findMany({ where: { userId, revokedAt: null, expiresAt: { gt: new Date() } }, select: { id: true, createdAt: true, expiresAt: true } }); }
  async revokeOtherSessions(userId: string, currentRefreshHash?: string) { await this.db.session.updateMany({ where: { userId, revokedAt: null, ...(currentRefreshHash ? { NOT: { refreshTokenHash: currentRefreshHash } } : {}) }, data: { revokedAt: new Date() } }); }
  async connectClient(input: { userId: string; deviceId: string; clientVersion: string; minecraftVersion: string }) { const client = await this.db.clientInstance.create({ data: input }); return client.id; }
  async heartbeatClient(id: string, data: { server?: string | null; latencyMs?: number }) { await this.db.clientInstance.update({ where: { id }, data: { lastHeartbeatAt: new Date(), serverAddress: data.server, latencyMs: data.latencyMs } }); }
  async disconnectClient(id: string) { await this.db.clientInstance.update({ where: { id }, data: { status: 'DISCONNECTED' } }); }
  listNotifications(userId: string, limit: number) { return this.db.notification.findMany({ where: { userId }, orderBy: { createdAt: 'desc' }, take: limit, select: { id: true, type: true, title: true, body: true, readAt: true, createdAt: true } }); }
  async markNotification(userId: string, id: string) { return (await this.db.notification.updateMany({ where: { id, userId, readAt: null }, data: { readAt: new Date() } })).count === 1; }
  async markAllNotifications(userId: string) { await this.db.notification.updateMany({ where: { userId, readAt: null }, data: { readAt: new Date() } }); }
  async disconnect() { await this.db.$disconnect(); }
}
