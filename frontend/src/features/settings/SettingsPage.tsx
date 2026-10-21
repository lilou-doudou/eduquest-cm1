import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { post } from '../../api/client';
import { setPreferences, usePreferences } from '../../app/preferences';
import { useMe, useSessionChanged } from '../../app/session';
import { Field } from '../../components/Field';
import { ErrorMessage } from '../../components/States';

function ParentAccess() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const sessionChanged = useSessionChanged();
  const access = useMutation({
    mutationFn: () => post('/auth/parent-session', { email, password }),
    onSuccess: async () => {
      await sessionChanged();
      navigate('/parent/enfants', { replace: true });
    },
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!access.isPending) access.mutate();
  };
  return (
    <form className="panel" onSubmit={submit} noValidate>
      <h2>Espace adulte</h2>
      <p>Réservé à l'adulte : e-mail et mot de passe du compte familial.</p>
      <Field label="Adresse e-mail de l'adulte" type="email" autoComplete="off" value={email} onChange={(e) => setEmail(e.target.value)} />
      <Field label="Mot de passe de l'adulte" type="password" autoComplete="off" value={password} onChange={(e) => setPassword(e.target.value)} />
      {access.error && <ErrorMessage error={access.error} />}
      <button type="submit" className="btn btn-secondary" disabled={access.isPending}>
        Ouvrir l'espace adulte
      </button>
    </form>
  );
}

export function SettingsPage() {
  const prefs = usePreferences();
  const { data: me } = useMe();
  return (
    <section>
      <h1>Paramètres</h1>
      <div className="panel">
        <div className="field-check">
          <input id="pref-sound" type="checkbox" checked={prefs.sound} onChange={(e) => setPreferences({ sound: e.target.checked })} />
          <label htmlFor="pref-sound">Sons de correction (désactivés par défaut)</label>
        </div>
        <div className="field-check">
          <input id="pref-motion" type="checkbox" checked={prefs.reducedMotion} onChange={(e) => setPreferences({ reducedMotion: e.target.checked })} />
          <label htmlFor="pref-motion">Réduire les animations</label>
        </div>
        <div className="field-check">
          <input id="pref-map" type="checkbox" checked={prefs.mapMode === '2d'} onChange={(e) => setPreferences({ mapMode: e.target.checked ? '2d' : 'auto' })} />
          <label htmlFor="pref-map">Toujours utiliser la carte 2D</label>
        </div>
      </div>
      {me?.role === 'CHILD' && <ParentAccess />}
    </section>
  );
}
