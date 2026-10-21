import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import type { Role } from '../api/types';
import { useMe } from '../app/session';
import { ErrorMessage, Loading } from './States';

/**
 * Garde d'interface uniquement : les autorisations réelles sont vérifiées par le serveur.
 * Elle évite d'afficher un écran qui recevrait de toute façon 401/403.
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { data: me, isPending, error, refetch } = useMe();
  const location = useLocation();
  if (isPending) return <Loading />;
  if (error) return <ErrorMessage error={error} onRetry={() => refetch()} />;
  if (!me) return <Navigate to="/connexion" replace state={{ from: location.pathname }} />;
  if (me.role !== role) {
    return <Navigate to={me.role === 'CHILD' ? '/aventure' : '/profils'} replace />;
  }
  return <>{children}</>;
}
