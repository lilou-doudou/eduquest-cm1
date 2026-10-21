import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { post } from '../../api/client';
import { Field } from '../../components/Field';
import { ErrorMessage, Loading } from '../../components/States';

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const verify = useMutation({ mutationFn: () => post('/auth/verify-email', { token }) });
  const started = useRef(false);
  useEffect(() => {
    if (!started.current && token) {
      started.current = true;
      verify.mutate();
    }
  }, [token, verify]);
  return (
    <section className="narrow">
      <h1>Confirmation de l'adresse</h1>
      {!token && <p>Lien incomplet.</p>}
      {verify.isPending && <Loading label="Vérification…" />}
      {verify.isSuccess && (
        <p role="status">
          Adresse confirmée ! <Link to="/connexion">Se connecter</Link>
        </p>
      )}
      {verify.error && <ErrorMessage error={verify.error} />}
    </section>
  );
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const forgot = useMutation({ mutationFn: () => post('/auth/forgot-password', { email }) });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!forgot.isPending) forgot.mutate();
  };
  return (
    <section className="narrow">
      <h1>Mot de passe oublié</h1>
      {forgot.isSuccess ? (
        <p role="status">Si un compte existe pour cette adresse, un lien valable 30 minutes vient d'être envoyé.</p>
      ) : (
        <form onSubmit={submit} noValidate>
          <Field label="Adresse e-mail" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          {forgot.error && <ErrorMessage error={forgot.error} />}
          <button type="submit" className="btn btn-primary" disabled={forgot.isPending}>
            Recevoir un lien
          </button>
        </form>
      )}
    </section>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [password, setPassword] = useState('');
  const reset = useMutation({ mutationFn: () => post('/auth/reset-password', { token: params.get('token') ?? '', newPassword: password }) });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!reset.isPending) reset.mutate();
  };
  return (
    <section className="narrow">
      <h1>Nouveau mot de passe</h1>
      {reset.isSuccess ? (
        <p role="status">
          Mot de passe modifié. Toutes les sessions ont été fermées. <Link to="/connexion">Se connecter</Link>
        </p>
      ) : (
        <form onSubmit={submit} noValidate>
          <Field
            label="Nouveau mot de passe"
            type="password"
            autoComplete="new-password"
            help="Entre 12 et 128 caractères."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {reset.error && <ErrorMessage error={reset.error} />}
          <button type="submit" className="btn btn-primary" disabled={reset.isPending}>
            Enregistrer
          </button>
        </form>
      )}
    </section>
  );
}
