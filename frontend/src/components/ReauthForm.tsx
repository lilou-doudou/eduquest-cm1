import { useState, type FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { post } from '../api/client';
import { Field } from './Field';
import { ErrorMessage } from './States';

/** Confirmation du mot de passe adulte avant une action sensible (valable 5 minutes côté serveur). */
export function ReauthForm({ onDone, onCancel, action }: { onDone: () => void; onCancel: () => void; action: string }) {
  const [password, setPassword] = useState('');
  const reauth = useMutation({ mutationFn: () => post('/auth/reauthenticate', { password }), onSuccess: onDone });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!reauth.isPending) reauth.mutate();
  };
  return (
    <form className="panel" onSubmit={submit} aria-label="Confirmation du mot de passe">
      <p>Pour {action}, confirme ton mot de passe adulte.</p>
      <Field label="Mot de passe" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} autoFocus />
      {reauth.error && <ErrorMessage error={reauth.error} />}
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={reauth.isPending}>
          Confirmer
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Annuler
        </button>
      </div>
    </form>
  );
}
