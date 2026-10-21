import { Link } from 'react-router';

/**
 * Notice d'information — PROJET à compléter avant toute ouverture publique (identité et coordonnées de
 * l'exploitant, hébergeur, bases légales validées). Voir docs/security-privacy.md.
 */
export function NoticePage() {
  return (
    <article className="prose">
      <h1>Confidentialité</h1>
      <p className="notice">
        Version 2026-10-adulte-v1 — document de travail : l'identité de l'exploitant, l'hébergement et les bases légales
        doivent être validés avant l'ouverture au public.
      </p>

      <h2>Pour l'enfant</h2>
      <div className="panel child-notice">
        <p>EduQuest garde seulement ce qu'il faut pour que tu puisses jouer et progresser :</p>
        <ul>
          <li>ton pseudonyme et ton avatar (pas ton vrai nom, pas ta photo) ;</li>
          <li>tes réponses aux questions, tes étoiles et tes récompenses ;</li>
          <li>rien n'est montré aux autres enfants : il n'y a pas de classement ni de chat.</li>
        </ul>
        <p>Ton parent peut voir tes progrès, télécharger tes données ou les effacer.</p>
      </div>

      <h2>Pour l'adulte</h2>
      <h3>Responsable de traitement</h3>
      <p>[À compléter : nom de l'exploitant, adresse, contact pour exercer les droits.]</p>
      <h3>Données traitées et finalités</h3>
      <ul>
        <li>
          <strong>Compte adulte</strong> : adresse e-mail, mot de passe haché (Argon2id), dates de création et de connexion.
          Finalité : gérer le compte familial. Base légale proposée : exécution du service demandé (à valider).
        </li>
        <li>
          <strong>Suivi pédagogique</strong> : profils enfants (pseudonyme, avatar, classe), réponses, recours aux indices,
          progression et récompenses. Finalité : fonctionnement des missions et suivi par l'adulte. Base légale proposée :
          exécution du service demandé par l'adulte (à valider).
        </li>
        <li>
          <strong>Sécurité</strong> : journal minimal (type d'événement, identifiant pseudonyme, résultat, date), limitation de
          tentatives. Finalité : protéger les comptes. Base légale proposée : intérêt légitime (à valider).
        </li>
        <li>
          <strong>Communications</strong> : e-mails strictement transactionnels (confirmation d'adresse, mot de passe oublié,
          compte inactif). Aucune newsletter, aucune publicité.
        </li>
      </ul>
      <p>
        Aucune donnée n'est demandée sur l'identité de l'enfant : pas de nom complet, de date de naissance, d'école,
        d'adresse, de photo ni de donnée de santé. Choisis un pseudonyme qui ne permet pas de l'identifier.
      </p>
      <h3>Cookies</h3>
      <p>
        Un seul cookie strictement nécessaire : <code>EQSESSION</code> (session, HttpOnly). Aucun traceur publicitaire ou de
        mesure d'audience. Les préférences d'affichage (son, animations, carte 2D) restent dans ton navigateur.
      </p>
      <h3>Durées de conservation proposées</h3>
      <ul>
        <li>Réponses et progression : pendant l'activité du compte.</li>
        <li>Après 12 mois sans connexion : e-mail d'avertissement, puis suppression 30 jours plus tard.</li>
        <li>Compte jamais confirmé : supprimé après 7 jours.</li>
        <li>Journaux de sécurité : 90 jours. Sauvegardes : rotation de 30 jours au maximum.</li>
      </ul>
      <h3>Tes droits</h3>
      <p>
        Depuis l'espace adulte (<Link to="/parent/confidentialite">Données</Link>) tu peux télécharger toutes les données de la
        famille, corriger un profil, supprimer un profil ou supprimer le compte. Pour les autres droits : [contact à compléter].
        Tu peux aussi adresser une réclamation à la CNIL.
      </p>
    </article>
  );
}
