import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { post } from '../../api/client';
import { useSessionChanged } from '../../app/session';
import { Field } from '../../components/Field';
import { ErrorMessage } from '../../components/States';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const sessionChanged = useSessionChanged();
  const expired = new URLSearchParams(location.search).has('expiree');
  const login = useMutation({
    mutationFn: () => post('/auth/login', { email, password }),
    onSuccess: async () => {
      await sessionChanged();
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from.startsWith('/parent') ? from : '/profils', { replace: true });
    },
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!login.isPending) login.mutate();
  };
  return (
    <section className="narrow">
      <h1>Connexion adulte</h1>
      {expired && (
        <p className="notice" role="status">
          La session a expiré. Reconnecte-toi pour continuer.
        </p>
      )}
      <form onSubmit={submit} noValidate>
        <Field label="Adresse e-mail" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <Field
          label="Mot de passe"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {login.error && <ErrorMessage error={login.error} />}
        <button type="submit" className="btn btn-primary" disabled={login.isPending}>
          {login.isPending ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
      <p>
        <Link to="/mot-de-passe-oublie">Mot de passe oublié ?</Link> · <Link to="/inscription">Créer un compte</Link>
      </p>
    </section>
  );
}
