import type { WorldChunk } from './types'

const DATABASE = 'kairokk-world-cache'
const STORE = 'chunks'
const VERSION = 1
const MAX_WORLD_CHUNKS = 2048
type CachedChunk = { id: string; worldKey: string; savedAt: number; chunk: WorldChunk }

let databasePromise: Promise<IDBDatabase> | null = null
let savesUntilPrune = 64

const database = () => {
  databasePromise ??= new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE, VERSION)
    request.onupgradeneeded = () => {
      const store = request.result.createObjectStore(STORE, { keyPath: 'id' })
      store.createIndex('worldKey', 'worldKey', { unique: false })
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
  return databasePromise
}

export const makeWorldCacheKey = (username: string, serverAddress: string, dimension: string) => `${username.toLowerCase()}::${serverAddress.toLowerCase()}::${dimension}`

export async function loadNearbyChunks(worldKey: string, centerX: number, centerZ: number, radius: number) {
  try {
    const db = await database()
    return await new Promise<WorldChunk[]>((resolve, reject) => {
      const request = db.transaction(STORE, 'readonly').objectStore(STORE).index('worldKey').getAll(IDBKeyRange.only(worldKey))
      request.onsuccess = () => resolve((request.result as CachedChunk[]).map((entry) => entry.chunk).filter((chunk) => Math.abs(chunk.x - centerX) <= radius && Math.abs(chunk.z - centerZ) <= radius))
      request.onerror = () => reject(request.error)
    })
  } catch { return [] }
}

export async function saveWorldChunk(worldKey: string, chunk: WorldChunk) {
  try {
    const db = await database()
    const transaction = db.transaction(STORE, 'readwrite')
    transaction.objectStore(STORE).put({ id: `${worldKey}::${chunk.key}`, worldKey, savedAt: Date.now(), chunk } satisfies CachedChunk)
    if (--savesUntilPrune > 0) return
    savesUntilPrune = 64
    const prune = db.transaction(STORE, 'readwrite'), store = prune.objectStore(STORE)
    const request = store.index('worldKey').getAll(IDBKeyRange.only(worldKey))
    request.onsuccess = () => {
      const overflow = (request.result as CachedChunk[]).sort((a, b) => b.savedAt - a.savedAt).slice(MAX_WORLD_CHUNKS)
      overflow.forEach((entry) => store.delete(entry.id))
    }
  } catch { /* Caching is an optional acceleration and must not interrupt World View. */ }
}
