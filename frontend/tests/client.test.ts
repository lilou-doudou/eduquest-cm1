import { describe, expect, it, vi } from 'vitest';
import { ApiError, onSessionExpired, post, get } from '../src/api/client';
import { mockApi } from './helpers';

describe('client HTTP', () => {
  it('envoie le jeton CSRF sur les mutations et pas sur les lectures', async () => {
    const calls = mockApi({
      'POST /api/v1/auth/logout': () => ({ status: 204 }),
      'GET /api/v1/worlds': () => ({ status: 200, body: [] }),
    });
    await get('/worlds');
    await post('/auth/logout');
    const logout = calls.find((c) => c.path === '/api/v1/auth/logout')!;
    expect(logout.headers['X-CSRF-TOKEN']).toBe('jeton-1');
    expect(calls.find((c) => c.path === '/api/v1/worlds')!.headers['X-CSRF-TOKEN']).toBeUndefined();
  });

  it('rejoue une seule fois après un jeton CSRF invalide', async () => {
    let attempts = 0;
    const calls = mockApi({
      'POST /api/v1/attempts/a1/complete': () => {
        attempts++;
        return attempts === 1 ? { status: 403, body: { code: 'CSRF_INVALID', detail: 'x' } } : { status: 200, body: { ok: true } };
      },
    });
    await expect(post('/attempts/a1/complete')).resolves.toEqual({ ok: true });
    expect(calls.filter((c) => c.path === '/api/v1/auth/csrf')).toHaveLength(2);
  });

  it('traduit les erreurs problem+json et signale la session expirée', async () => {
    mockApi({ 'GET /api/v1/me/progress': () => ({ status: 401, body: { code: 'SESSION_REQUIRED', detail: 'Session absente ou expirée.' } }) });
    const listener = vi.fn();
    const off = onSessionExpired(listener);
    const error = (await get('/me/progress').catch((e: unknown) => e)) as ApiError;
    off();
    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('SESSION_REQUIRED');
    expect(listener).toHaveBeenCalledOnce();
  });

  it("transforme une panne réseau en erreur lisible", async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));
    const error = (await get('/worlds').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('NETWORK_ERROR');
    expect(error.message).toMatch(/Connexion impossible/);
  });
});
