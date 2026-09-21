import type { BlockKind, Vec3, WorldChunk, WorldEntity, WorldMetadata, WorldPacket, WorldPlayer } from './types'

const heightAt = (x: number, z: number) => Math.floor(3 + Math.sin(x * .22) * 1.4 + Math.cos(z * .18) * 1.2 + Math.sin((x + z) * .08))
const kindAt = (_x: number, z: number, y: number, top: number): BlockKind => y === top ? (Math.abs(z + 8) < 3 ? 'sand' : 'grass') : y > top - 2 ? 'dirt' : 'stone'
const tree = (x: number, y: number, z: number) => { const blocks = []; for (let i = 1; i < 5; i++) blocks.push({ x, y: y + i, z, kind: 'wood' as const }); for (let dx = -2; dx <= 2; dx++) for (let dz = -2; dz <= 2; dz++) for (let dy = 3; dy <= 5; dy++) if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) < 5) blocks.push({ x: x + dx, y: y + dy, z: z + dz, kind: 'leaves' as const }); return blocks }
export const buildMockChunks = (radius = 2): WorldChunk[] => {
  const chunks: WorldChunk[] = []
  for (let cx = -radius; cx <= radius; cx++) for (let cz = -radius; cz <= radius; cz++) {
    const blocks: WorldChunk['blocks'] = []
    for (let lx = 0; lx < 16; lx++) for (let lz = 0; lz < 16; lz++) { const x = cx * 16 + lx, z = cz * 16 + lz, top = heightAt(x, z); for (let y = Math.max(0, top - 2); y <= top; y++) blocks.push({ x, y, z, kind: kindAt(x, z, y, top) }); if ((x * 13 + z * 29) % 23 === 0 && Math.abs(z + 8) >= 3) blocks.push({ x, y: top + 1, z, kind: 'plant' }); if ((x * 17 + z * 31) % 157 === 0 && Math.abs(x) > 5) blocks.push(...tree(x, top, z)); }
    chunks.push({ key: `${cx},${cz}`, x: cx, z: cz, blocks, revision: 1 })
  }
  return chunks
}
export const mockMetadata: WorldMetadata = { dimension: 'Overworld', biome: 'Plains', facing: 'South', light: 15, time: 'Day (6000)', expectedChunks: 25 }
export const mockPlayer: WorldPlayer = { uuid: '8667ba71-b85a-4004-af54-457a9734eed7', name: 'Kairokk', x: 0, y: heightAt(0, 0) + 1, z: 0, yaw: 176, pitch: 4, velocity: { x: 0, y: 0, z: 0 }, onGround: true, sprinting: false, sneaking: false }
export const mockEntities: WorldEntity[] = [{ id: 'sheep-1', type: 'Sheep', x: 8, y: heightAt(8, 4) + 1, z: 4, health: 8 }, { id: 'zombie-1', type: 'Zombie', x: -11, y: heightAt(-11, 7) + 1, z: 7, health: 20, hostile: true }, { id: 'item-1', type: 'Dropped Item', x: 3, y: heightAt(3, -8) + 1, z: -8 }]
export const mockPath: Vec3[] = Array.from({ length: 18 }, (_, i) => ({ x: i * 1.25, y: heightAt(Math.floor(i * 1.25), Math.floor(-i * .55)) + 1.15, z: -i * .55 }))

export const createMockWorldProvider = (emit: (packet: WorldPacket) => void) => {
  let stopped = false; const timers: number[] = []; const chunks = buildMockChunks();
  emit({ type: 'world:init', sessionId: crypto.randomUUID(), expectedChunks: chunks.length });
  chunks.forEach((chunk, index) => timers.push(window.setTimeout(() => !stopped && emit({ type: 'world:chunk', chunk }), index * 45)));
  timers.push(window.setTimeout(() => { if (stopped) return; emit({ type: 'world:metadata', metadata: mockMetadata }); mockEntities.forEach((entity) => emit({ type: 'world:entity-spawn', entity })); emit({ type: 'world:path', nodes: mockPath }) }, 220));
  let phase = 0; const movement = window.setInterval(() => { if (stopped) return; phase += .12; emit({ type: 'world:player', player: { ...mockPlayer, x: Math.sin(phase) * 3, z: Math.cos(phase) * 3, yaw: (phase * 57) % 360, velocity: { x: Math.cos(phase) * .15, y: 0, z: -Math.sin(phase) * .15 } } }); emit({ type: 'world:entity-update', entity: { ...mockEntities[0], x: 8 + Math.sin(phase * .7) * 2, z: 4 + Math.cos(phase * .7) * 2 } }) }, 250);
  return () => { stopped = true; timers.forEach(window.clearTimeout); window.clearInterval(movement) }
}
