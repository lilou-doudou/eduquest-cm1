import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { get, post } from '../../api/client';
import type { Child } from '../../api/types';
import { useSessionChanged } from '../../app/session';
import { Avatar } from '../../components/Avatar';
import { ErrorMessage, Loading } from '../../components/States';
import { CHILDREN_KEY } from './ChildrenPage';

/** Choix du profil : uniquement depuis la session adulte. Le serveur remplace alors la session par une session enfant. */
export function ProfilesPage() {
  const children = useQuery({ queryKey: CHILDREN_KEY, queryFn: () => get<Child[]>('/children') });
  const navigate = useNavigate();
  const sessionChanged = useSessionChanged();
  const enter = useMutation({
    mutationFn: (childId: string) => post('/auth/child-session', { childId }),
    onSuccess: async () => {
      await sessionChanged();
      navigate('/aventure', { replace: true });
    },
  });
  return (
    <section>
      <h1>Qui joue ?</h1>
      {children.isPending && <Loading />}
      {children.error && <ErrorMessage error={children.error} onRetry={() => children.refetch()} />}
      {children.data && children.data.length === 0 && (
        <p>
          Aucun profil pour l'instant. <Link to="/parent/enfants">Créer un profil enfant</Link>
        </p>
      )}
      {children.data && children.data.length > 0 && (
        <>
          <ul className="profile-grid">
            {children.data.map((c) => (
              <li key={c.id}>
                <button type="button" className="profile-button" onClick={() => enter.mutate(c.id)} disabled={enter.isPending}>
                  <Avatar code={c.avatarCode} size={96} decorative />
                  <span>{c.nickname}</span>
                </button>
              </li>
            ))}
          </ul>
          <p className="field-help">
            En mode enfant, l'espace adulte est protégé : il faudra ressaisir l'e-mail et le mot de passe pour y revenir.
          </p>
        </>
      )}
      {enter.error && <ErrorMessage error={enter.error} />}
      <p>
        <Link to="/parent/enfants">Gérer les profils et voir le suivi</Link>
      </p>
    </section>
  );
}
