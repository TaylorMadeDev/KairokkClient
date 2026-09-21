import { resolve } from 'node:path';
import argon2 from 'argon2';
import { buildApp } from './app.js';
import { loadEnv } from './lib/env.js';
import { LocalStore } from './lib/local-store.js';
import { PrismaStore } from './lib/store.js';

const env = loadEnv();
const store = env.STORAGE_MODE === 'postgres' ? new PrismaStore() : new LocalStore(resolve(env.KAIROKK_DATA_FILE));
if (store instanceof LocalStore) await store.ensureDefaultAdmin(await argon2.hash('admin', { type: argon2.argon2id, memoryCost: 19456, timeCost: 2, parallelism: 1 }));
const app = await buildApp(env, store);
const close = async () => { await app.close(); await store.disconnect(); };
process.once('SIGINT', close); process.once('SIGTERM', close);
await app.listen({ port: env.PORT, host: env.HOST });
