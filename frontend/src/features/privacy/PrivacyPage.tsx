import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ApiError, del, get } from '../../api/client';
import { useSessionChanged } from '../../app/session';
import { Field } from '../../components/Field';
import { ReauthForm } from '../../components/ReauthForm';
import { ErrorMessage, Loading } from '../../components/States';

interface Ack {
  notice_version: string;
  purpose_code: string;
  parent_action: string;
  recorded_at: string;
  child_nickname: string | null;
}

async function downloadExport() {
  const res = await fetch('/api/v1/privacy/export', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.code ?? `HTTP_${res.status}`, body.detail ?? 'Export impossible.');
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `eduquest-export-${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
}

export function PrivacyPage() {
  const acks = useQuery({ queryKey: ['privacy', 'acks'], queryFn: () => get<Ack[]>('/privacy/acknowledgements') });
  const [reauthFor, setReauthFor] = useState<'export' | 'delete' | null>(null);
  const [confirmation, setConfirmation] = useState('');
  const navigate = useNavigate();
  const sessionChanged = useSessionChanged();

  const exportData = useMutation({
    mutationFn: downloadExport,
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'REAUTHENTICATION_REQUIRED') setReauthFor('export');
    },
  });
  const deleteAccount = useMutation({
    mutationFn: () => del('/account', { confirmation }),
    onSuccess: async () => {
      await sessionChanged();
      navigate('/', { replace: true });
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'REAUTHENTICATION_REQUIRED') setReauthFor('delete');
    },
  });
  const needsReauth = (e: unknown) => e instanceof ApiError && e.code === 'REAUTHENTICATION_REQUIRED';

  return (
    <section>
      <h1>Données de la famille</h1>
      <p>
        Lis la <Link to="/confidentialite">notice d'information</Link> (adulte et enfant).
      </p>

      {reauthFor && (
        <ReauthForm
          action={reauthFor === 'export' ? 'télécharger les données' : 'supprimer le compte'}
          onCancel={() => setReauthFor(null)}
          onDone={() => {
            const which = reauthFor;
            setReauthFor(null);
            if (which === 'export') exportData.mutate();
            else deleteAccount.mutate();
          }}
        />
      )}

      <h2>Télécharger les données</h2>
      <p>Un fichier JSON contenant le compte, les profils, les parties, les réponses, la progression et les récompenses.</p>
      <button type="button" className="btn btn-primary" onClick={() => exportData.mutate()} disabled={exportData.isPending}>
        Télécharger l'export
      </button>
      {exportData.isSuccess && <p role="status">Export téléchargé.</p>}
      {exportData.error && !needsReauth(exportData.error) && <ErrorMessage error={exportData.error} />}

      <h2>Historique des informations délivrées</h2>
      {acks.isPending && <Loading />}
      {acks.error && <ErrorMessage error={acks.error} onRetry={() => acks.refetch()} />}
      {acks.data && (
        <ul>
          {acks.data.map((a, i) => (
            <li key={i}>
              {new Date(a.recorded_at).toLocaleDateString('fr-FR')} : notice {a.notice_version}
              {a.child_nickname ? ` (profil ${a.child_nickname})` : ''} — {a.purpose_code === 'ACCOUNT' ? 'compte adulte' : 'suivi pédagogique'}
            </li>
          ))}
        </ul>
      )}

      <h2>Supprimer le compte</h2>
      <form
        className="panel panel-danger"
        onSubmit={(e) => {
          e.preventDefault();
          if (!deleteAccount.isPending) deleteAccount.mutate();
        }}
      >
        <p>
          La suppression efface immédiatement le compte, tous les profils, réponses, progressions, récompenses et sessions. Les
          sauvegardes expirent sous 30 jours au plus.
        </p>
        <Field label="Écris SUPPRIMER pour confirmer" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} />
        {deleteAccount.error && !needsReauth(deleteAccount.error) && <ErrorMessage error={deleteAccount.error} />}
        <button type="submit" className="btn btn-danger" disabled={confirmation !== 'SUPPRIMER' || deleteAccount.isPending}>
          Supprimer définitivement le compte
        </button>
      </form>
    </section>
  );
}
