export type Role = 'USER' | 'ADMIN';
export type User = { id: string; username: string; email: string; passwordHash: string; role: Role; isDisabled: boolean; createdAt: Date; updatedAt: Date; lastLoginAt: Date | null };
export type Session = { id: string; userId: string; refreshTokenHash: string; createdAt: Date; expiresAt: Date; revokedAt: Date | null };
export type Device = { id: string; userId: string; deviceFingerprintHash: string; deviceName: string; platform: string; firstSeenAt: Date; lastSeenAt: Date; isAuthorized: boolean };
