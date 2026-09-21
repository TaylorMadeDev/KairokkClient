import { createHash, randomBytes } from 'node:crypto';
export const sha256 = (value: string) => createHash('sha256').update(value).digest('hex');
export const opaqueToken = () => randomBytes(48).toString('base64url');
export const hashRefreshToken = (token: string, pepper: string) => sha256(`${pepper}:${token}`);
