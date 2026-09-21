const baseUrl = import.meta.env.VITE_API_URL ?? '';
const tokenKey = 'kairokk_access_token';
let accessToken = localStorage.getItem(tokenKey) ?? sessionStorage.getItem(tokenKey);
let refreshRequest: Promise<string> | null = null;
export const worldSocketUrl = () => { if (!accessToken) return null; const origin = baseUrl ? new URL(baseUrl, window.location.origin).origin : window.location.origin; return `${origin.replace(/^http/, 'ws')}/world/connect?accessToken=${encodeURIComponent(accessToken)}` };
export const minecraftSkinUrl = (uuid: string) => `${baseUrl}/minecraft/skin/${encodeURIComponent(uuid)}`;

export type DashboardData = { statistics: { playTime: string; sessions: number; blocksTravelled: number; macrosUsed: number }; configs: number; scripts: number; activity: { id: string; type: string; metadata: unknown; createdAt: string }[]; client: { status: 'ONLINE' | 'OFFLINE' | 'CONNECTING'; version: string | null; serverAddress: string | null } };

export const setToken = (token: string | null, remember = true) => {
  accessToken = token;
  localStorage.removeItem(tokenKey);
  sessionStorage.removeItem(tokenKey);
  if (token) (remember ? localStorage : sessionStorage).setItem(tokenKey, token);
};

const parseResponse = async <T>(response: Response): Promise<T> => {
  if (response.status === 204) return undefined as T;
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.error?.message ?? 'The server could not complete that request.');
  return body as T;
};

const refreshAccessToken = () => {
  refreshRequest ??= fetch(`${baseUrl}/auth/refresh`, {
    method: 'POST', credentials: 'include', headers: { 'content-type': 'application/json' }, body: '{}',
  }).then((response) => parseResponse<{ accessToken: string }>(response))
    .then(({ accessToken: token }) => {
      const remember = localStorage.getItem(tokenKey) !== null;
      setToken(token, remember);
      return token;
    })
    .finally(() => { refreshRequest = null; });
  return refreshRequest;
};

export const request = async <T>(path: string, init: RequestInit = {}, retry = true): Promise<T> => {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      credentials: 'include',
      headers: { 'content-type': 'application/json', ...(accessToken ? { authorization: `Bearer ${accessToken}` } : {}), ...init.headers },
    });
  } catch {
    throw new Error('Kairokk could not reach the account service. Please try again in a moment.');
  }
  if (response.status === 401 && retry && path !== '/auth/login' && path !== '/auth/register' && path !== '/auth/refresh') {
    try {
      await refreshAccessToken();
      return request<T>(path, init, false);
    } catch {
      setToken(null);
    }
  }
  return parseResponse<T>(response);
};

export const api = {
  register: (input: unknown) => request<{ accessToken: string }>('/auth/register', { method: 'POST', body: JSON.stringify(input) }),
  login: (input: unknown) => request<{ accessToken: string }>('/auth/login', { method: 'POST', body: JSON.stringify(input) }),
  logout: () => request<void>('/auth/logout', { method: 'POST', body: '{}' }),
  clearSession: () => setToken(null),
  me: () => request<{ user: { username: string; email: string } }>('/auth/me'),
  dashboard: () => request<DashboardData>('/api/dashboard'),
  command: (type: string, payload: unknown = {}) => request('/api/client/commands', { method: 'POST', body: JSON.stringify({ type, payload }) }),
  configs: () => request<{ configs: Config[] }>('/api/configs'),
  saveConfig: (name: string, data: unknown) => request<{ config: Config }>('/api/configs', { method: 'POST', body: JSON.stringify({ name, data }) }),
  removeConfig: (id: string) => request<void>(`/api/configs/${id}`, { method: 'DELETE' }),
  scripts: () => request<{ scripts: Script[] }>('/api/scripts'),
  saveScript: (input: Partial<Script> & Pick<Script, 'name' | 'content' | 'enabled'>) => request<{ script: Script }>(input.id ? `/api/scripts/${input.id}` : '/api/scripts', { method: input.id ? 'PUT' : 'POST', body: JSON.stringify(input) }),
  removeScript: (id: string) => request<void>(`/api/scripts/${id}`, { method: 'DELETE' }),
  account: () => request<Account>('/api/account'),
  updateAccount: (input: unknown) => request<{ user: { username: string } }>('/api/account', { method: 'PATCH', body: JSON.stringify(input) }),
  revokeOthers: () => request('/api/account/sessions/revoke-others', { method: 'POST' }),
};

export type Config = { id: string; name: string; data: unknown; updatedAt: string };
export type Script = { id: string; name: string; content: string; enabled: boolean; updatedAt: string };
export type Account = { user: { username: string; email: string; createdAt: string; role: string }; devices: { id: string; deviceName: string; platform: string; lastSeenAt: string }[]; sessions: { id: string; createdAt: string; expiresAt: string }[]; client: DashboardData['client'] };
