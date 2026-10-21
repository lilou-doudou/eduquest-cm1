import type { ReactNode } from 'react';
import { ApiError } from '../api/client';

export function Loading({ label = 'Chargement…' }: { label?: string }) {
  return (
    <p className="state state-loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" /> {label}
    </p>
  );
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return error.message;
    if (error.status === 429) return `Trop de tentatives. Réessaie dans ${error.retryAfter ?? 60} secondes.`;
    if (error.status >= 500) return 'Le service rencontre un problème. Réessaie dans un moment.';
    return error.message;
  }
  return 'Une erreur inattendue est survenue.';
}

export function ErrorMessage({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div className="state state-error" role="alert">
      <p>{errorMessage(error)}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Réessayer
        </button>
      )}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="state state-empty">{children}</p>;
}
