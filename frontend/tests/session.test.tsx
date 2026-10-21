import { describe, expect, it } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { createQueryClient } from '../src/app/App';
import { ME_KEY, useSessionChanged } from '../src/app/session';
import { PARENT_ME } from './helpers';

describe('changement de session', () => {
  it('purge la progression et les récompenses du précédent enfant', async () => {
    const client = createQueryClient();
    client.setQueryData(ME_KEY, PARENT_ME);
    client.setQueryData(['me', 'progress'], { nickname: 'Nova', xpTotal: 70 });
    client.setQueryData(['me', 'rewards'], { rewards: ['FIRST_STEP'] });
    client.setQueryData(['worlds'], []);
    const { result } = renderHook(() => useSessionChanged(), {
      wrapper: ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>,
    });
    await act(async () => { await result.current(); });
    expect(client.getQueryData(['me', 'progress'])).toBeUndefined();
    expect(client.getQueryData(['me', 'rewards'])).toBeUndefined();
    expect(client.getQueryData(['worlds'])).toBeUndefined();
    expect(client.getQueryData(ME_KEY)).toEqual(PARENT_ME);
    client.clear();
  });
});
