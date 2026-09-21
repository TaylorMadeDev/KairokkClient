import type { WebSocket } from 'ws';
import { z } from 'zod';

const vec3 = z.object({ x: z.number().finite(), y: z.number().finite(), z: z.number().finite() });
const block = vec3.extend({ kind: z.string().min(1).max(128) });
const chunk = z.object({ key: z.string().max(64), x: z.number().int(), z: z.number().int(), revision: z.number().int().nonnegative(), blocks: z.array(block).max(200_000) });
export const worldEvent = z.discriminatedUnion('event', [
  z.object({ event: z.literal('WORLD_INIT'), data: z.object({ sessionId: z.string().uuid(), expectedChunks: z.number().int().min(0).max(289), centerX: z.number().int().optional(), centerZ: z.number().int().optional(), metadata: z.record(z.unknown()).optional() }) }),
  z.object({ event: z.literal('WORLD_CHUNK'), data: z.object({ chunk }) }),
  z.object({ event: z.literal('WORLD_CHUNK_UNLOAD'), data: z.object({ key: z.string().max(64) }) }),
  z.object({ event: z.literal('WORLD_BLOCK_UPDATE'), data: z.object({ chunkKey: z.string().max(64), position: vec3, block: block.nullable() }) }),
  z.object({ event: z.literal('WORLD_PLAYER_STATE'), data: z.object({ player: vec3.extend({ uuid: z.string().uuid().optional(), name: z.string().min(1).max(16).optional(), yaw: z.number(), pitch: z.number(), velocity: vec3, onGround: z.boolean(), sprinting: z.boolean(), sneaking: z.boolean(), usingItem: z.boolean().optional(), swimming: z.boolean().optional(), fallFlying: z.boolean().optional() }) }) }),
  z.object({ event: z.enum(['WORLD_ENTITY_SPAWN', 'WORLD_ENTITY_UPDATE']), data: z.object({ entity: vec3.extend({ id: z.string().max(128), type: z.string().max(128), health: z.number().optional(), hostile: z.boolean().optional() }) }) }),
  z.object({ event: z.literal('WORLD_ENTITY_REMOVE'), data: z.object({ id: z.string().max(128) }) }),
  z.object({ event: z.literal('WORLD_METADATA'), data: z.object({ metadata: z.record(z.unknown()) }) }),
  z.object({ event: z.literal('PATHFINDER_PATH'), data: z.object({ nodes: z.array(vec3).max(4096) }) }),
  z.object({ event: z.literal('WORLD_RESET'), data: z.object({ reason: z.string().max(128) }) })
]);

const browserNames: Record<string, string> = { WORLD_INIT: 'world:init', WORLD_CHUNK: 'world:chunk', WORLD_CHUNK_UNLOAD: 'world:chunk-unload', WORLD_BLOCK_UPDATE: 'world:block-update', WORLD_PLAYER_STATE: 'world:player', WORLD_ENTITY_SPAWN: 'world:entity-spawn', WORLD_ENTITY_UPDATE: 'world:entity-update', WORLD_ENTITY_REMOVE: 'world:entity-remove', WORLD_METADATA: 'world:metadata', PATHFINDER_PATH: 'world:path', WORLD_RESET: 'world:reset' };
export class WorldViewHub {
  private readonly subscribers = new Map<string, Set<WebSocket>>();
  private readonly replay = new Map<string, Map<string, unknown>>();
  subscribe(userId: string, socket: WebSocket) { const set = this.subscribers.get(userId) ?? new Set<WebSocket>(); set.add(socket); this.subscribers.set(userId, set); for (const message of this.replay.get(userId)?.values() ?? []) socket.send(JSON.stringify(message)); }
  unsubscribe(userId: string, socket: WebSocket) { const set = this.subscribers.get(userId); set?.delete(socket); if (!set?.size) this.subscribers.delete(userId); }
  hasSubscribers(userId: string) { return (this.subscribers.get(userId)?.size ?? 0) > 0; }
  clear(userId: string) { this.replay.delete(userId); }
  receive(userId: string, input: unknown) { const parsed = worldEvent.parse(input); const type = browserNames[parsed.event]; const data = parsed.data as Record<string, unknown>; const message = { type, ...data }; if (parsed.event === 'WORLD_RESET' || parsed.event === 'WORLD_INIT') this.clear(userId); const replay = this.replay.get(userId) ?? new Map<string, unknown>(); this.replay.set(userId, replay); const key = parsed.event === 'WORLD_CHUNK' ? `chunk:${(data.chunk as { key: string }).key}` : parsed.event === 'WORLD_ENTITY_SPAWN' || parsed.event === 'WORLD_ENTITY_UPDATE' ? `entity:${(data.entity as { id: string }).id}` : parsed.event; if (parsed.event === 'WORLD_CHUNK_UNLOAD') replay.delete(`chunk:${String(data.key)}`); else if (parsed.event === 'WORLD_ENTITY_REMOVE') replay.delete(`entity:${String(data.id)}`); else replay.set(key, message); for (const socket of this.subscribers.get(userId) ?? []) if (socket.readyState === socket.OPEN) socket.send(JSON.stringify(message)); }
}
