# Kairokk World View protocol v1

World View is an authenticated, subscription-driven reconstruction protocol. It never captures or streams the screen or opens a local server. The backend's live replay state remains ephemeral; the dashboard may retain a bounded browser-local terrain cache as described below.

## Lifecycle

1. The browser opens `/world/connect?accessToken=...` with its existing short-lived dashboard token.
2. The browser sends `WORLD_SUBSCRIBE` with a radius from 1 to 8 chunks.
3. The backend derives the user from the token and forwards the command only to that user's authorized Minecraft socket.
4. Minecraft clears its ephemeral state, emits `WORLD_INIT`, then sends nearest chunks first.
5. Changed player state is sampled at up to 10 Hz; unchanged state is refreshed every two seconds. Metadata and Pathfinder state are change/rate limited.
6. Closing World View sends `WORLD_UNSUBSCRIBE`. Backend or Minecraft disconnects clear all server-side replay state.

`WORLD_RESYNC_REQUEST` restarts the same lifecycle. Dimension and server changes emit/reset the active session before a new initialization.

## Client events

All v1 client packets use the existing realtime `EVENT` envelope. `payload.event` is one of:

- `WORLD_INIT`: UUID session ID, expected chunk count, center chunk coordinates, and metadata.
- `WORLD_CHUNK`: `{ key, x, z, revision, blocks }`. Blocks contain primitive `x/y/z/kind` values captured on the Minecraft thread and encoded on the dedicated worker.
- `WORLD_CHUNK_UNLOAD`: chunk key.
- `WORLD_BLOCK_UPDATE`: chunk key, position, replacement block or null.
- `WORLD_PLAYER_STATE`: position, rotation, velocity, ground/sprint/sneak state.
- `WORLD_ENTITY_SPAWN`, `WORLD_ENTITY_UPDATE`, `WORLD_ENTITY_REMOVE`: nearby non-local entities with stable UUIDs, registry type, position, optional health, and hostile classification.
- `WORLD_METADATA`: dimension, biome label, facing, light, time, expected chunks.
- `PATHFINDER_PATH`: simplified route nodes.
- `WORLD_RESET`: reason for invalidating the current view.

The backend validates every event with Zod, holds only the latest ephemeral replay state per authenticated user, and maps names to the browser-facing `world:*` events. A browser cannot supply a user, device, client, or session ID to change ownership.

After accepting a `WORLD_CHUNK`, the backend returns `WORLD_CHUNK_ACK` with its key and revision. Minecraft keeps at most six chunks in flight, retries unacknowledged chunks after 15 seconds, and retries chunks that have not yet loaded locally. A chunk is considered synchronized only after the matching acknowledgment. The browser shows the received count against the requested count; chunks outside Minecraft's loaded view cannot be reconstructed until the game receives them.

## Browser terrain cache and sync completion

For multiplayer only, the dashboard stores received chunk payloads in IndexedDB. Entries are isolated by dashboard username, server address, and Minecraft dimension. On a later visit it restores only chunks inside the newly announced center window, then replaces those entries as live chunks arrive. Singleplayer terrain, players, entities, Pathfinder routes, and other transient state are never cached. Each world is capped at 2,048 chunks and older entries are pruned periodically.

The initial sync completes immediately at the advertised count. If an edge chunk is not available from the Minecraft client, it also completes after 1.8 seconds with no newly received or restored chunks. This prevents an otherwise healthy 80/81 reconstruction from displaying a permanent syncing overlay while retaining the received terrain.

## Rendering and threading

The browser groups blocks by chunk and material, face-culls covered opaque blocks, and uses `InstancedMesh` rather than a component/draw call per block. Removed and revised chunks dispose their GPU resources independently.

Minecraft reads world objects only during the client tick. It copies primitive block data into a bounded queue. A single worker performs JSON construction/network submission. The radius is capped at eight, the encoder queue at 24 chunks, and unacknowledged chunks at six; nearby chunks are queued first. When the player is underground, capture follows a band around their height so the reconstructed scene includes the nearby cave instead of only distant surface terrain.

Nearby entities are sampled at 4 Hz inside the subscribed chunk window. The client emits spawns for newly observed UUIDs, updates only after position/health/type changes, and removals when an entity leaves the window or despawns. The local player is omitted because it already uses `WORLD_PLAYER_STATE` and its dedicated marker.

## Current block format

The client transmits the complete namespaced block registry ID as `kind` (for example `minecraft:lily_pad` or `minecraft:gold_pressure_plate`). The browser accepts any registry ID, groups common material families by color, and renders thin geometry for lily pads, carpets, pressure plates, slabs, and other known shapes. Unknown blocks receive a stable fallback color. This is a stylized reconstruction; full blockstate properties, vanilla textures, and every exact collision/visual model are not yet transmitted. Older cached category names remain readable.

Each column is scanned from the terrain floor through its `WORLD_SURFACE` height, so trunks, ground, and plants beneath leaf canopies remain present. The navigation planner recognizes lily pads, carpets, and pressure plates as thin support. Water is never a route node: gaps between pads require reachable jumps in arbitrary horizontal directions with a sampled collision sweep, and touching water stops the route immediately. A jump waypoint advances only after the player lands on its target block cell.

The protocol keeps `revision` and a replaceable chunk payload boundary so a later v2 palette/section binary encoder can be introduced without changing ownership, lifecycle, browser subscription, or renderer architecture. Full section palettes, binary compression, block batches, entity interest management, bandwidth shaping, and measured cache tuning remain subsequent phases and should not be represented as complete until profiled in both singleplayer and multiplayer.
