// Client HTTP typé : cookies de session HttpOnly (jamais de jeton en localStorage), en-tête CSRF
// sur toutes les mutations, erreurs application/problem+json.

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: { field: string; message: string }[];
  readonly retryAfter: number | null;

  constructor(status: number, code: string, detail: string, fieldErrors: { field: string; message: string }[] = [], retryAfter: number | null = null) {
    super(detail);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.retryAfter = retryAfter;
  }
}

let csrf: { headerName: string; token: string } | null = null;
let csrfPromise: Promise<{ headerName: string; token: string }> | null = null;

/** À appeler après tout changement de session (connexion, mode enfant, déconnexion). */
export function resetCsrf() {
  csrf = null;
  csrfPromise = null;
}

async function ensureCsrf() {
  if (csrf) return csrf;
  if (!csrfPromise) {
    csrfPromise = fetch('/api/v1/auth/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } })
      .then(async (res) => {
        if (!res.ok) throw new ApiError(res.status, 'CSRF_UNAVAILABLE', 'Service indisponible.');
        csrf = await res.json();
        return csrf!;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }
  return csrfPromise;
}

type Listener = () => void;
const sessionExpiredListeners = new Set<Listener>();
export function onSessionExpired(listener: Listener) {
  sessionExpiredListeners.add(listener);
  return () => {
    sessionExpiredListeners.delete(listener);
  };
}

async function toError(res: Response): Promise<ApiError> {
  let body: { code?: string; detail?: string; fieldErrors?: { field: string; message: string }[] } = {};
  try {
    body = await res.json();
  } catch {
    // corps absent ou non JSON
  }
  const retry = res.headers.get('Retry-After');
  return new ApiError(
    res.status,
    body.code ?? `HTTP_${res.status}`,
    body.detail ?? 'Une erreur est survenue.',
    body.fieldErrors ?? [],
    retry ? Number(retry) : null,
  );
}

export async function api<T>(path: string, options: { method?: string; body?: unknown; retried?: boolean } = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (method !== 'GET') {
    const token = await ensureCsrf();
    headers[token.headerName] = token.token;
    headers['Content-Type'] = 'application/json';
  }
  let res: Response;
  try {
    res = await fetch(`/api/v1${path}`, {
      method,
      headers,
      credentials: 'same-origin',
      body: method === 'GET' ? undefined : JSON.stringify(options.body ?? {}),
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Connexion impossible. Vérifie le réseau puis réessaie.');
  }
  if (res.ok) {
    if (res.status === 204 || res.status === 202) return undefined as T;
    return (await res.json()) as T;
  }
  const error = await toError(res);
  if (error.code === 'CSRF_INVALID' && !options.retried) {
    // Jeton expiré avec l'ancienne session : on en obtient un nouveau et on rejoue une seule fois.
    resetCsrf();
    return api<T>(path, { ...options, retried: true });
  }
  if (res.status === 401 && error.code === 'SESSION_REQUIRED') {
    sessionExpiredListeners.forEach((l) => l());
  }
  throw error;
}

export const get = <T>(path: string) => api<T>(path);
export const post = <T>(path: string, body: unknown = {}) => api<T>(path, { method: 'POST', body });
export const put = <T>(path: string, body: unknown = {}) => api<T>(path, { method: 'PUT', body });
export const patch = <T>(path: string, body: unknown = {}) => api<T>(path, { method: 'PATCH', body });
export const del = <T>(path: string, body: unknown = {}) => api<T>(path, { method: 'DELETE', body });
