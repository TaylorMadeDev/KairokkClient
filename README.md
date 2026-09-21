# Kairokk

Kairokk is a single repository containing its Minecraft client, dashboard, and service layer.

## Projects

- [`Minecraft Client`](./Minecraft%20Client/) — Fabric/ModernUI client for Minecraft 26.1.2 (Java 25).
- [`Frontend`](./Frontend/) — Vite/React dashboard.
- [`server`](./server/) — Fastify/TypeScript API and realtime service.

The projects are intentionally kept as siblings so each can be built and run independently. See each project’s README for setup and verification commands. The World View socket protocol is documented in [`WORLD_VIEW_PROTOCOL.md`](./WORLD_VIEW_PROTOCOL.md).
