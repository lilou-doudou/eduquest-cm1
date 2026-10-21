import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError, get, resetCsrf } from '../api/client';
import type { Me } from '../api/types';

export const ME_KEY = ['me'] as const;

/** Contexte de session reconstruit depuis le serveur ; null si aucune session. */
export function useMe() {
  return useQuery<Me | null>({
    queryKey: ME_KEY,
    queryFn: async () => {
      try {
        return await get<Me>('/auth/me');
      } catch (e) {
        if (e instanceof ApiError && (e.status === 401 || e.status === 403)) return null;
        throw e;
      }
    },
    staleTime: 60_000,
    retry: false,
  });
}

/** Après un changement de privilège : nouveau jeton CSRF et cache vidé (aucune donnée d'un autre contexte). */
export function useSessionChanged() {
  const qc = useQueryClient();
  return async () => {
    resetCsrf();
    qc.removeQueries({ predicate: (q) => !(q.queryKey.length === 1 && q.queryKey[0] === 'me') });
    await qc.invalidateQueries({ queryKey: ME_KEY });
    await qc.refetchQueries({ queryKey: ME_KEY });
  };
}

let expired = false;

/** Mémorise qu'une session vient d'expirer, pour que les gardes affichent le bon message. */
export function markSessionExpired() {
  expired = true;
}

export function consumeSessionExpired(): boolean {
  const value = expired;
  expired = false;
  return value;
}
