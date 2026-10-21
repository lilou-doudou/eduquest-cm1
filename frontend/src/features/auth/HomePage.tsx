import { Link, Navigate } from 'react-router';
import { useMe } from '../../app/session';

export function HomePage() {
  const { data: me } = useMe();
  if (me?.role === 'CHILD') return <Navigate to="/aventure" replace />;
  return (
    <section className="hero">
      <h1>EduQuest CM1</h1>
      <p className="lead">
        Une aventure en 3D pour s'entraîner en mathématiques et en anglais : des missions courtes de cinq questions, des
        explications à chaque erreur et des récompenses à collectionner.
      </p>
      <ul className="hero-points">
        <li>Séances de 5 à 10 minutes, sans minuteur.</li>
        <li>Deux mondes ouverts : Les Îles des Nombres et Le Royaume des Mots. Quatre autres arrivent bientôt.</li>
        <li>Un espace adulte pour suivre les progrès, sans classement entre enfants.</li>
        <li>Sans publicité, sans achat, sans chat.</li>
      </ul>
      <div className="actions">
        {me?.role === 'PARENT' ? (
          <Link className="btn btn-primary" to="/profils">
            Choisir un profil
          </Link>
        ) : (
          <>
            <Link className="btn btn-primary" to="/inscription">
              Créer un compte parent
            </Link>
            <Link className="btn btn-secondary" to="/connexion">
              Se connecter
            </Link>
          </>
        )}
      </div>
      <p>
        <Link to="/confidentialite">Comment sont utilisées les données ?</Link>
      </p>
    </section>
  );
}
