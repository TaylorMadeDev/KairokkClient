export type Vec3 = { x: number; y: number; z: number }
export type BlockKind = string // Namespaced Minecraft registry ID; legacy cached categories also render.
export type WorldBlock = Vec3 & { kind: BlockKind }
export type WorldChunk = { key: string; x: number; z: number; blocks: WorldBlock[]; revision: number }
export type WorldPlayer = Vec3 & {
  uuid?: string
  name?: string
  yaw: number
  pitch: number
  velocity: Vec3
  onGround: boolean
  sprinting: boolean
  sneaking: boolean
  usingItem?: boolean
  swimming?: boolean
  fallFlying?: boolean
}
export type WorldEntity = Vec3 & { id: string; type: string; health?: number; hostile?: boolean }
export type WorldMetadata = { dimension: string; biome: string; facing: string; light: number; time: string; expectedChunks: number }
export type WorldSnapshot = { sessionId: string; chunks: Map<string, WorldChunk>; player: WorldPlayer; entities: Map<string, WorldEntity>; metadata: WorldMetadata; path: Vec3[] }
export type WorldPacket =
  | { type: 'world:init'; sessionId: string; expectedChunks: number; centerX?: number; centerZ?: number; metadata?: Partial<WorldMetadata> }
  | { type: 'world:chunk'; chunk: WorldChunk }
  | { type: 'world:chunk-unload'; key: string }
  | { type: 'world:block-update'; chunkKey: string; block: WorldBlock | null; position: Vec3 }
  | { type: 'world:player'; player: WorldPlayer }
  | { type: 'world:entity-spawn' | 'world:entity-update'; entity: WorldEntity }
  | { type: 'world:entity-remove'; id: string }
  | { type: 'world:metadata'; metadata: Partial<WorldMetadata> }
  | { type: 'world:path'; nodes: Vec3[] }
  | { type: 'world:reset'; reason: string }
