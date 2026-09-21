import type { WebSocket } from 'ws';
import { AppError } from '../lib/errors.js';
import { commandMessage, commandTypes } from './protocol.js';
type Connection = { userId: string; deviceId: string; socket: WebSocket; capabilities: string[] };
export class RealtimeHub { private readonly clients = new Map<string, Connection>(); private readonly pending = new Map<string, { resolve: (value: unknown) => void; reject: (reason: Error) => void; timer: NodeJS.Timeout }>();
  connect(connection: Connection) { this.clients.set(connection.userId, connection); }
  disconnect(userId: string) { this.clients.delete(userId); }
  async command(userId: string, requestId: string, command: string, payload: unknown) { const allowed = commandTypes.safeParse(command); const connection = this.clients.get(userId); if (!allowed.success) throw new AppError('COMMAND_NOT_ALLOWED', 400, 'That client command is not supported.'); if (!connection || connection.socket.readyState !== connection.socket.OPEN) throw new AppError('CLIENT_OFFLINE', 409, 'The Kairokk client is offline.'); if (!connection.capabilities.includes(command)) throw new AppError('COMMAND_NOT_SUPPORTED', 409, 'This client version does not support that command.'); connection.socket.send(JSON.stringify(commandMessage(requestId, allowed.data, payload))); return new Promise((resolve, reject) => { const timer = setTimeout(() => { this.pending.delete(requestId); reject(new AppError('COMMAND_TIMEOUT', 504, 'The client did not respond in time.')); }, 10_000); this.pending.set(requestId, { resolve, reject, timer }); }); }
  result(requestId: string, result: unknown) { const pending = this.pending.get(requestId); if (pending) { clearTimeout(pending.timer); this.pending.delete(requestId); pending.resolve(result); } }
}
