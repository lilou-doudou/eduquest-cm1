import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ApiError, get, post } from '../../api/client';
import { Field } from '../../components/Field';
import { ErrorMessage, Loading } from '../../components/States';

export function RegisterPage() {
  const notice = useQuery({ queryKey: ['notice'], queryFn: () => get<{ noticeVersion: string }>('/privacy/notice') });
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [read, setRead] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const register = useMutation({
    mutationFn: () => post('/auth/register', { email, password, noticeVersion: notice.data!.noticeVersion }),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    if (password.length < 12) return setLocalError('Le mot de passe doit contenir au moins 12 caractères.');
    if (!read) return setLocalError("Merci de lire la notice d'information avant de continuer.");
    if (!register.isPending) register.mutate();
  };
  if (notice.isPending) return <Loading />;
  if (notice.error) return <ErrorMessage error={notice.error} onRetry={() => notice.refetch()} />;
  if (register.isSuccess) {
    return (
      <section className="narrow">
        <h1>Vérifie ta boîte e-mail</h1>
        <p role="status">
          Si cette adresse peut être utilisée, un lien de confirmation vient d'être envoyé. Il est valable 24 heures.
        </p>
        <p>
          <Link to="/connexion">Aller à la connexion</Link>
        </p>
      </section>
    );
  }
  const fieldError = (name: string) =>
    register.error instanceof ApiError ? register.error.fieldErrors.find((f) => f.field === name)?.message : undefined;
  return (
    <section className="narrow">
      <h1>Créer un compte parent</h1>
      <p>Le compte appartient à l'adulte. Les enfants n'ont pas d'adresse e-mail : tu créeras ensuite leurs profils.</p>
      <form onSubmit={submit} noValidate>
        <Field label="Adresse e-mail" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} error={fieldError('email')} />
        <Field
          label="Mot de passe"
          type="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={128}
          help="Entre 12 et 128 caractères. Une phrase facile à retenir fonctionne bien."
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldError('password')}
        />
        <div className="field-check">
          <input id="notice" type="checkbox" checked={read} onChange={(e) => setRead(e.target.checked)} />
          <label htmlFor="notice">
            J'ai lu la <Link to="/confidentialite" target="_blank">notice d'information</Link> (version {notice.data.noticeVersion}).
          </label>
        </div>
        {localError && (
          <p className="field-error" role="alert">
            {localError}
          </p>
        )}
        {register.error && <ErrorMessage error={register.error} />}
        <button type="submit" className="btn btn-primary" disabled={register.isPending}>
          {register.isPending ? 'Envoi…' : 'Créer le compte'}
        </button>
      </form>
    </section>
  );
}
