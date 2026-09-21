import { z } from 'zod';

const schema = z.object({
  STORAGE_MODE: z.enum(['local', 'postgres']).default('local'),
  KAIROKK_DATA_FILE: z.string().default('.data/kairokk.json'),
  DATABASE_URL: z.string().url().default('postgresql://kairokk:kairokk@localhost:5432/kairokk?schema=public'),
  JWT_SECRET: z.string().min(32).default('kairokk-local-jwt-secret-change-in-production'),
  REFRESH_TOKEN_PEPPER: z.string().min(32).default('kairokk-local-refresh-pepper-change-in-production'),
  CORS_ORIGIN: z.string().url().default('http://localhost:5173'),
  PORT: z.coerce.number().int().positive().default(3000),
  HOST: z.string().default('127.0.0.1'),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  COOKIE_SECURE: z.enum(['true', 'false']).default('false').transform((value) => value === 'true')
});
export type Env = z.infer<typeof schema>;
export const loadEnv = (input = process.env): Env => schema.parse(input);
