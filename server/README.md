# Kairokk server

This folder contains the dashboard API, PostgreSQL schema, secure authentication, device binding, and WebSocket command transport for the Minecraft client.

## Start locally

Run `npm install` and `npm run dev`. Local development needs no database setup; accounts are persisted in `.data/kairokk.json`.

Local storage seeds an administrator account on first start with username `admin` and password `admin`. This development-only seed is never created when `STORAGE_MODE=postgres`.

For production, copy `.env.example` to `.env`, set `STORAGE_MODE=postgres`, provide the database URL and two unique secrets, run `npm run prisma:generate`, then apply the schema with `npm run prisma:migrate -- --name init`.

Run the quality gates with `npm run lint`, `npm run typecheck`, and `npm test`.

## Authentication

`POST /auth/register` and `POST /auth/login` return a 15-minute access token and set a strict, HTTP-only refresh cookie. The response also includes the refresh token for the native Minecraft client; browser callers should prefer the cookie. Each refresh revokes the old hashed session before issuing a new one. `POST /auth/logout` revokes that session. Auth endpoints are rate limited.

## Devices and realtime

The client sends only a locally-created SHA-256 device-fingerprint hash to `POST /devices/register`. The service stores no raw hardware identifiers and allows one authorized device by default. A second device receives `DEVICE_LIMIT_REACHED`; it is never silently swapped. The dashboard can remove an owned device with `DELETE /devices/:id`.

The client connects to `GET /client/connect?deviceFingerprintHash=...` with an `Authorization: Bearer <access-token>` handshake header, sends a versioned `HELLO`, and follows with heartbeats. The server validates declared capabilities and correlates every remote command with a `COMMAND_RESULT` response and a ten-second timeout.

Minecraft currently executes HUD toggling, Item ESP toggling, chat messages, and pathfinder start/stop. Unsupported commands are rejected instead of being reported as successful.
