import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { AppError } from '../lib/errors.js';

type CachedSkin = { body: Buffer; contentType: string; expiresAt: number };
const cache = new Map<string, CachedSkin>();
const uuidSchema = z.string().regex(/^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[1-5][0-9a-fA-F]{3}-?[89abAB][0-9a-fA-F]{3}-?[0-9a-fA-F]{12}$/);
const textureUrlSchema = z.string().url().refine((value) => {
  const url = new URL(value);
  return (url.protocol === 'https:' || url.protocol === 'http:') && url.hostname === 'textures.minecraft.net' && /^\/texture\/[0-9a-f]+$/i.test(url.pathname);
});

export const minecraftRoutes = async (app: FastifyInstance) => {
  app.get('/minecraft/skin/:uuid', async (request, reply) => {
    const uuid = uuidSchema.parse((request.params as { uuid: unknown }).uuid).replaceAll('-', '').toLowerCase();
    const cached = cache.get(uuid);
    if (cached && cached.expiresAt > Date.now()) return reply.type(cached.contentType).header('cache-control', 'public, max-age=3600').send(cached.body);

    const profileResponse = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`);
    if (!profileResponse.ok) throw new AppError('MINECRAFT_PROFILE_NOT_FOUND', 404, 'Minecraft profile skin was not found.');
    const profile = await profileResponse.json() as { properties?: { name?: string; value?: string }[] };
    const encoded = profile.properties?.find((property) => property.name === 'textures')?.value;
    if (!encoded) throw new AppError('MINECRAFT_SKIN_NOT_FOUND', 404, 'Minecraft profile does not have a skin.');
    const textures = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8')) as { textures?: { SKIN?: { url?: string } } };
    const textureUrl = new URL(textureUrlSchema.parse(textures.textures?.SKIN?.url)); textureUrl.protocol = 'https:';
    const skinResponse = await fetch(textureUrl);
    if (!skinResponse.ok) throw new AppError('MINECRAFT_SKIN_UNAVAILABLE', 502, 'Minecraft skin service is unavailable.');
    const body = Buffer.from(await skinResponse.arrayBuffer());
    const contentType = skinResponse.headers.get('content-type') ?? 'image/png';
    cache.set(uuid, { body, contentType, expiresAt: Date.now() + 60 * 60 * 1000 });
    return reply.type(contentType).header('cache-control', 'public, max-age=3600').send(body);
  });
};
