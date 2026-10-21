import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError, del, get, patch, post } from '../../api/client';
import type { Child } from '../../api/types';
import { AVATARS, Avatar } from '../../components/Avatar';
import { Field } from '../../components/Field';
import { ReauthForm } from '../../components/ReauthForm';
import { Empty, ErrorMessage, Loading } from '../../components/States';

export const CHILDREN_KEY = ['children'];

function AvatarPicker({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <fieldset className="avatar-picker">
      <legend>Avatar</legend>
      {Object.entries(AVATARS).map(([code, a]) => (
        <label key={code} className={value === code ? 'selected' : undefined}>
          <input type="radio" name="avatar" value={code} checked={value === code} onChange={() => onChange(code)} />
          <Avatar code={code} size={56} decorative />
          <span>{a.label}</span>
        </label>
      ))}
    </fieldset>
  );
}

function ChildForm({ initial, onSaved, onCancel }: { initial?: Child; onSaved: () => void; onCancel?: () => void }) {
  const qc = useQueryClient();
  const [nickname, setNickname] = useState(initial?.nickname ?? '');
  const [avatarCode, setAvatarCode] = useState(initial?.avatarCode ?? 'fox');
  const save = useMutation({
    mutationFn: () =>
      initial
        ? patch<Child>(`/children/${initial.id}`, { nickname, avatarCode })
        : post<Child>('/children', { nickname, avatarCode, grade: 'CM1' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CHILDREN_KEY });
      onSaved();
    },
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!save.isPending) save.mutate();
  };
  const nicknameError = save.error instanceof ApiError ? save.error.fieldErrors.find((f) => f.field === 'nickname')?.message : undefined;
  return (
    <form className="panel" onSubmit={submit} noValidate>
      <Field
        label="Pseudonyme"
        help="Un surnom qui ne permet pas d'identifier l'enfant (pas de nom complet). 2 à 30 caractères."
        value={nickname}
        maxLength={30}
        onChange={(e) => setNickname(e.target.value)}
        error={nicknameError}
      />
      <AvatarPicker value={avatarCode} onChange={setAvatarCode} />
      <p className="field-help">Classe : CM1</p>
      {save.error && !nicknameError && <ErrorMessage error={save.error} />}
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={save.isPending}>
          {initial ? 'Enregistrer' : 'Créer le profil'}
        </button>
        {onCancel && (
          <button type="button" className="btn btn-secondary" onClick={onCancel}>
            Annuler
          </button>
        )}
      </div>
    </form>
  );
}

function DeleteChild({ child, onDone }: { child: Child; onDone: () => void }) {
  const qc = useQueryClient();
  const [step, setStep] = useState<'confirm' | 'reauth'>('confirm');
  const [confirmation, setConfirmation] = useState('');
  const remove = useMutation({
    mutationFn: () => del(`/children/${child.id}`, { confirmation }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CHILDREN_KEY });
      onDone();
    },
    onError: (e) => {
      if (e instanceof ApiError && e.code === 'REAUTHENTICATION_REQUIRED') setStep('reauth');
    },
  });
  if (step === 'reauth') {
    return (
      <ReauthForm
        action={`supprimer le profil de ${child.nickname}`}
        onCancel={onDone}
        onDone={() => {
          setStep('confirm');
          remove.mutate();
        }}
      />
    );
  }
  return (
    <form
      className="panel panel-danger"
      onSubmit={(e) => {
        e.preventDefault();
        if (!remove.isPending) remove.mutate();
      }}
    >
      <p>
        Supprimer le profil de <strong>{child.nickname}</strong> efface définitivement ses parties, ses réponses, sa progression
        et ses récompenses.
      </p>
      <Field label="Écris SUPPRIMER pour confirmer" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} />
      {remove.error && !(remove.error instanceof ApiError && remove.error.code === 'REAUTHENTICATION_REQUIRED') && (
        <ErrorMessage error={remove.error} />
      )}
      <div className="actions">
        <button type="submit" className="btn btn-danger" disabled={confirmation !== 'SUPPRIMER' || remove.isPending}>
          Supprimer définitivement
        </button>
        <button type="button" className="btn btn-secondary" onClick={onDone}>
          Annuler
        </button>
      </div>
    </form>
  );
}

export function ChildrenPage() {
  const children = useQuery({ queryKey: CHILDREN_KEY, queryFn: () => get<Child[]>('/children') });
  const [mode, setMode] = useState<{ kind: 'none' } | { kind: 'create' } | { kind: 'edit' | 'delete'; child: Child }>({ kind: 'none' });
  return (
    <section>
      <h1>Ma famille</h1>
      <p>Jusqu'à cinq profils enfants. Aucune adresse e-mail ni donnée d'identité n'est demandée pour un enfant.</p>
      {children.isPending && <Loading />}
      {children.error && <ErrorMessage error={children.error} onRetry={() => children.refetch()} />}
      {children.data && children.data.length === 0 && mode.kind !== 'create' && <Empty>Aucun profil pour le moment.</Empty>}
      {children.data && (
        <ul className="card-list">
          {children.data.map((c) => (
            <li key={c.id} className="card child-card">
              <Avatar code={c.avatarCode} size={56} decorative />
              <div className="grow">
                <h2>{c.nickname}</h2>
                <p>CM1</p>
              </div>
              <div className="actions">
                <Link className="btn btn-secondary" to={`/parent/suivi/${c.id}`}>
                  Voir le suivi
                </Link>
                <button type="button" className="btn btn-secondary" onClick={() => setMode({ kind: 'edit', child: c })}>
                  Modifier
                </button>
                <button type="button" className="btn btn-ghost" onClick={() => setMode({ kind: 'delete', child: c })}>
                  Supprimer
                </button>
              </div>
              {mode.kind === 'edit' && mode.child.id === c.id && (
                <ChildForm initial={c} onSaved={() => setMode({ kind: 'none' })} onCancel={() => setMode({ kind: 'none' })} />
              )}
              {mode.kind === 'delete' && mode.child.id === c.id && <DeleteChild child={c} onDone={() => setMode({ kind: 'none' })} />}
            </li>
          ))}
        </ul>
      )}
      {mode.kind === 'create' ? (
        <>
          <h2>Nouveau profil</h2>
          <ChildForm onSaved={() => setMode({ kind: 'none' })} onCancel={() => setMode({ kind: 'none' })} />
        </>
      ) : (
        children.data &&
        children.data.length < 5 && (
          <button type="button" className="btn btn-primary" onClick={() => setMode({ kind: 'create' })}>
            Ajouter un profil
          </button>
        )
      )}
    </section>
  );
}
