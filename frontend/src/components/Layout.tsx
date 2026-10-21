import { useEffect, useRef, type ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { post } from '../api/client';
import { useMe, useSessionChanged } from '../app/session';
import { Avatar } from './Avatar';

/** Restaure le focus sur le titre principal à chaque changement d'écran. */
function useFocusOnNavigate(ref: React.RefObject<HTMLElement | null>) {
  const location = useLocation();
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    const heading = ref.current?.querySelector<HTMLElement>('h1');
    if (heading) {
      heading.setAttribute('tabindex', '-1');
      heading.focus();
    }
  }, [location.pathname, ref]);
}

export function Layout({ children }: { children: ReactNode }) {
  const { data: me } = useMe();
  const mainRef = useRef<HTMLElement>(null);
  const navigate = useNavigate();
  const sessionChanged = useSessionChanged();
  useFocusOnNavigate(mainRef);
  const logout = useMutation({
    mutationFn: () => post('/auth/logout'),
    onSettled: async () => {
      await sessionChanged();
      navigate('/');
    },
  });

  return (
    <div className="app">
      <a className="skip-link" href="#contenu">
        Aller au contenu
      </a>
      <header className="topbar">
        <Link to={me?.role === 'CHILD' ? '/aventure' : '/'} className="brand">
          <img src="/assets/favicon.svg" alt="" width="32" height="32" />
          <span>EduQuest CM1</span>
        </Link>
        <nav aria-label="Navigation principale">
          <ul>
            {me?.role === 'CHILD' && (
              <>
                <li>
                  <NavLink to="/aventure">Carte</NavLink>
                </li>
                <li>
                  <NavLink to="/mon-aventurier" className="nav-avatar">
                    <Avatar code={me.child!.avatarCode} size={28} decorative /> {me.child!.nickname}
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/parametres">Paramètres</NavLink>
                </li>
              </>
            )}
            {me?.role === 'PARENT' && (
              <>
                <li>
                  <NavLink to="/profils">Jouer</NavLink>
                </li>
                <li>
                  <NavLink to="/parent/enfants">Ma famille</NavLink>
                </li>
                <li>
                  <NavLink to="/parent/confidentialite">Données</NavLink>
                </li>
                <li>
                  <button type="button" className="link-button" onClick={() => logout.mutate()} disabled={logout.isPending}>
                    Se déconnecter
                  </button>
                </li>
              </>
            )}
            {!me && (
              <>
                <li>
                  <NavLink to="/connexion">Connexion</NavLink>
                </li>
                <li>
                  <NavLink to="/inscription">Créer un compte</NavLink>
                </li>
              </>
            )}
          </ul>
        </nav>
      </header>
      <main id="contenu" ref={mainRef}>
        {children}
      </main>
      <footer className="footer">
        <Link to="/confidentialite">Confidentialité</Link>
        <span aria-hidden="true">·</span>
        <span>Sans publicité, sans achat, sans classement.</span>
      </footer>
    </div>
  );
}
