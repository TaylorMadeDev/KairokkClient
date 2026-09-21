export class AppError extends Error {
  constructor(public readonly code: string, public readonly statusCode: number, message: string) { super(message); }
}
export const errorBody = (error: unknown) => error instanceof AppError
  ? { error: { code: error.code, message: error.message } }
  : { error: { code: 'INTERNAL_ERROR', message: 'An unexpected error occurred.' } };
