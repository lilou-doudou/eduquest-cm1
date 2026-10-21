import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { vi } from 'vitest';
import { createQueryClient } from '../src/app/App';
import { resetCsrf } from '../src/api/client';

export interface MockCall {
  method: string;
  path: string;
  body: unknown;
  headers: Record<string, string>;
}

type Handler = (call: MockCall) => { status: number; body?: unknown } | Promise<{ status: number; body?: unknown }>;

/** Faux serveur : table « MÉTHODE /chemin » → réponse. Enregistre chaque appel. */
export function mockApi(routes: Record<string, Handler>) {
  resetCsrf();
  const calls: MockCall[] = [];
  const all: Record<string, Handler> = {
    'GET /api/v1/auth/csrf': () => ({ status: 200, body: { headerName: 'X-CSRF-TOKEN', token: 'jeton-1' } }),
    ...routes,
  };
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const path = url.replace(/^https?:\/\/[^/]+/, '').split('?')[0];
    const method = init?.method ?? 'GET';
    const call: MockCall = {
      method,
      path,
      body: init?.body ? JSON.parse(init.body as string) : undefined,
      headers: (init?.headers as Record<string, string>) ?? {},
    };
    calls.push(call);
    const key = Object.keys(all).find((k) => {
      const [m, p] = k.split(' ');
      return m === method && new RegExp(`^${p.replace(/:[a-zA-Z]+/g, '[^/]+')}$`).test(path);
    });
    if (!key) return new Response(JSON.stringify({ code: 'NOT_FOUND', detail: 'introuvable' }), { status: 404 });
    const res = await all[key](call);
    const status = res.status;
    return new Response(status === 204 || res.body === undefined ? null : JSON.stringify(res.body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    });
  });
  return calls;
}

export function renderAt(ui: ReactElement, path: string) {
  const client = createQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

export const CHILD_ME = { role: 'CHILD', parent: null, child: { id: 'c1', nickname: 'Nova', avatarCode: 'fox' } };
export const PARENT_ME = { role: 'PARENT', parent: { id: 'p1', email: 'adulte@exemple.test', emailVerified: true }, child: null };
