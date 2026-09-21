import { describe, expect, it } from 'vitest';
import { WorldViewHub } from '../src/realtime/world.js';

const socket = () => { const messages: unknown[] = []; return { messages, socket: { OPEN: 1, readyState: 1, send: (value: string) => messages.push(JSON.parse(value)) } as never }; };
describe('World View ownership and replay', () => {
  it('relays only to subscribers owned by the same authenticated user', () => {
    const hub = new WorldViewHub(); const first = socket(); const second = socket(); hub.subscribe('user-a', first.socket); hub.subscribe('user-b', second.socket);
    hub.receive('user-a', { event: 'WORLD_INIT', data: { sessionId: '00000000-0000-4000-8000-000000000001', expectedChunks: 1, centerX: 12, centerZ: -8 } });
    hub.receive('user-a', { event: 'WORLD_PLAYER_STATE', data: { player: { x: 1, y: 64, z: 2, yaw: 0, pitch: 0, velocity: { x: 0, y: 0, z: 0 }, onGround: true, sprinting: false, sneaking: false } } });
    expect(first.messages).toHaveLength(2); expect(second.messages).toHaveLength(0);
    expect(first.messages[0]).toMatchObject({ type: 'world:init', centerX: 12, centerZ: -8 });
  });
  it('drops unloaded chunks from replay', () => {
    const hub = new WorldViewHub(); hub.receive('user-a', { event: 'WORLD_CHUNK', data: { chunk: { key: '0,0', x: 0, z: 0, revision: 1, blocks: [] } } }); hub.receive('user-a', { event: 'WORLD_CHUNK_UNLOAD', data: { key: '0,0' } }); const target = socket(); hub.subscribe('user-a', target.socket); expect(target.messages).toHaveLength(0);
  });
  it('replays the latest entity state and removes entities that leave range', () => {
    const hub = new WorldViewHub();
    const entity = { id: '00000000-0000-4000-8000-000000000010', type: 'cow', x: 4, y: 70, z: 8, health: 10, hostile: false };
    hub.receive('user-a', { event: 'WORLD_ENTITY_SPAWN', data: { entity } });
    hub.receive('user-a', { event: 'WORLD_ENTITY_UPDATE', data: { entity: { ...entity, x: 5 } } });
    const first = socket(); hub.subscribe('user-a', first.socket);
    expect(first.messages).toEqual([{ type: 'world:entity-update', entity: { ...entity, x: 5 } }]);
    hub.receive('user-a', { event: 'WORLD_ENTITY_REMOVE', data: { id: entity.id } });
    const second = socket(); hub.subscribe('user-a', second.socket);
    expect(second.messages).toHaveLength(0);
  });
});
