import { randomUUID } from 'node:crypto';
import { AppError } from '../lib/errors.js';
type Stream = { id: string; userId: string; deviceId: string; expiresAt: number; state: 'REQUESTING' | 'CONNECTING' | 'LIVE' };
export class StreamManager { private readonly streams = new Map<string, Stream>();
  request(userId: string, deviceId: string) { const id = randomUUID(); const stream = { id, userId, deviceId, state: 'REQUESTING' as const, expiresAt: Date.now() + 60_000 }; this.streams.set(id, stream); return stream; }
  get(userId: string, id: string) { const stream = this.streams.get(id); if (!stream || stream.userId !== userId || stream.expiresAt < Date.now()) throw new AppError('STREAM_NOT_FOUND', 404, 'Stream session was not found.'); return stream; }
  stop(userId: string, id: string) { this.get(userId, id); this.streams.delete(id); }
}
