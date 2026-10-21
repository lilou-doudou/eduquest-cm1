# Sécurité et données personnelles

Ce document décrit les mesures implémentées et ce qui reste à décider. **Rien ici ne constitue une attestation de conformité au RGPD** : les points de la dernière section doivent être validés (juriste ou DPO, exploitant identifié) avant tout pilote public.

## Mesures techniques implémentées

| Exigence | Mise en œuvre | Preuve |
|---|---|---|
| Mots de passe | Argon2id (Spring Security + BouncyCastle) : sel 16 octets, hachage 32 octets, parallélisme 1, **19 456 Kio, 2 itérations** (minimum OWASP, configurable par `eduquest.security.argon2.*`). 12 à 128 caractères, espaces autorisés, jamais tronqués. Connexion d'un compte inconnu : hachage factice pour un temps de réponse comparable. | `AuthAndSecurityIntegrationTest.passwordPolicyAndNoticeVersionAreEnforced` |
| Session | Session serveur opaque (Spring Session JDBC), cookie `EQSESSION` HttpOnly, SameSite=Lax, Secure hors profil `dev`. Nouvelle session à la connexion, au passage en mode enfant et au retour adulte. | `sessionCookieIsHttpOnlyAndRenewedOnLogin`, `logoutRevokesServerSession` |
| Expiration | Inactivité 30 min (adulte) / 60 min (enfant) ; durée absolue 12 h appliquée par filtre avec horloge injectable. | `absoluteSessionLifetimeIsTwelveHours` |
| CSRF | Jeton de session exigé sur toutes les mutations, y compris connexion et déconnexion (`X-CSRF-TOKEN`, obtenu par `GET /auth/csrf`, renouvelé avec la session). Aucune mutation en GET. | `mutationsWithoutCsrfAreRefused` |
| Rate limit | Compteurs persistés en base (`rate_limit_bucket`), 429 + `Retry-After`, sans blocage définitif de compte. | `repeatedFailedLoginsAreRateLimitedWithRetryAfter` |
| Autorisations | Politique explicite par route, refus par défaut ; propriété vérifiée dans chaque service ; ressource d'une autre famille → 404 ; session enfant → 403 sur les routes adulte. | `IsolationIntegrationTest` (toutes les routes portant un identifiant) |
| Retour à l'espace adulte | Depuis le mode enfant : e-mail et mot de passe du parent propriétaire, rate limit renforcé. | `returnToParentRequiresOwnerCredentials` |
| Actions sensibles | Export, suppression de compte et suppression d'un profil exigent une authentification de moins de 5 minutes (`POST /auth/reauthenticate`) et, pour les suppressions, la saisie de « SUPPRIMER ». | `exportAndDeletionRequireRecentReauthentication`, `deletingAChildRequiresRecentReauthentication` |
| Jetons e-mail | Aléatoires, stockés en SHA-256, usage unique ; vérification 24 h, réinitialisation 30 min ; une réinitialisation révoque toutes les sessions. | `passwordResetRevokesAllSessions`, `resetTokenExpiresAfterThirtyMinutes` |
| Corrigés | Jamais dans le catalogue ni dans le bundle ; correction renvoyée seulement après la soumission. | `catalogNeverExposesAnswerKeys`, test Vitest du contrat des DTO |
| Erreurs | `application/problem+json` avec `code` et `traceId`, sans trace de pile. | `errorsAreProblemJsonWithoutStackTrace` |
| Entrées | DTO validés, champs inconnus refusés (`fail-on-unknown-properties`), en-têtes 16 Ko, corps de formulaire 64 Ko, requêtes paramétrées, pas d'upload, pas de HTML dans les contenus. | — |
| En-têtes | CSP `default-src 'self'` (ressources locales uniquement), `nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`, HSTS en profil `prod`. Export `Cache-Control: no-store`. | — |
| Production | `ProductionGuard` refuse de démarrer avec un mot de passe de base vide ou de démonstration, une `APP_BASE_URL` non HTTPS ou les données de démonstration. Swagger/OpenAPI désactivés hors `dev`. | `ProductionGuardTest`, vérifié aussi avec l'image Docker |
| Journaux | Pas de corps de requête, réponse d'enfant, cookie, jeton ou mot de passe journalisés. `audit_event` ne contient qu'un type, un résultat, une référence pseudonyme et un identifiant de corrélation. | Revue de code |

### Limites de débit

| Action | Par compte | Par adresse IP* |
|---|---|---|
| Connexion | 10 / 15 min | 30 / 15 min |
| Inscription | — | 10 / h |
| Renvoi d'e-mail de vérification | 3 / h | — |
| Mot de passe oublié | 3 / h | 10 / h |
| Réinitialisation | — | 20 / h |
| Réauthentification | 10 / 15 min | — |
| Retour à l'espace adulte | 5 / 15 min | 10 / 15 min |

\* multipliées par `eduquest.rate-limit.ip-factor` (1 par défaut) pour les réseaux partagés (NAT d'école) et les tests.

## Données traitées

| Donnée | Personne | Finalité | Durée proposée |
|---|---|---|---|
| E-mail, hachage du mot de passe, dates de connexion et d'activité | Adulte | Compte familial, sécurité | Durée de vie du compte |
| Pseudonyme (2–30 caractères), avatar, classe CM1 | Enfant | Profil de jeu | Durée de vie du profil |
| Parties, réponses, indices, progression, XP, récompenses, équipement | Enfant | Suivi pédagogique | Durée de vie du profil |
| Historique des informations délivrées (version de notice, finalité, base légale proposée) | Adulte, enfant | Traçabilité | Durée de vie du compte |
| Journal de sécurité (`audit_event`) | Référence pseudonyme | Sécurité | 90 jours |
| Registre de suppression (`deletion_record` : identifiant supprimé et date) | — | Réappliquer les suppressions après restauration | 35 jours |
| Compteurs de limitation (clé et compteur) | IP ou compte | Sécurité | 1 jour |

Aucune donnée d'identité de l'enfant n'est demandée (ni nom complet, ni date de naissance, ni école, ni photo). Aucun traceur ni outil d'analyse ; le seul cookie est le cookie de session strictement nécessaire. Les préférences d'affichage restent dans le `localStorage` du navigateur.

## Droits et procédures

- **Export** : *Données de la famille → Télécharger l'export* (`GET /api/v1/privacy/export`) : JSON versionné contenant compte, profils, parties, réponses, progression, récompenses et historique d'information ; ni hachage, ni jeton.
- **Rectification** : e-mail via support (non automatisé au MVP), pseudonyme et avatar modifiables par le parent.
- **Suppression d'un profil** : *Ma famille → Supprimer* ; cascade sur toutes ses données.
- **Suppression du compte** : *Données → Supprimer définitivement le compte* ; révocation de toutes les sessions, suppression immédiate et atomique de la base active, journaux d'audit dé-référencés, inscription dans `deletion_record`. Les données d'une autre famille ne sont pas touchées (`accountDeletionRemovesEverythingOfThatFamilyOnly`).
- **Purges planifiées** (`RetentionJob`, chaque jour à 03:30 UTC, testées avec horloge contrôlée) :
  - après 12 mois d'inactivité : e-mail à l'adulte, puis suppression 30 jours plus tard sauf reconnexion ;
  - comptes jamais vérifiés : supprimés après 7 jours ;
  - journal de sécurité : 90 jours ; jetons expirés et compteurs : 1 jour ; registre de suppression : 35 jours.
- **Sauvegardes** : rotation maximale 30 jours (à mettre en place par l'hébergeur) ; toute restauration doit réappliquer `deletion_record` (voir [deployment.md](deployment.md)).

## Notices

La notice adulte et la version enfant sont affichées sur `/confidentialite` (versions `2026-10-adulte-v1` et `2026-10-enfant-v1`). **Ce sont des brouillons** : les champs entre crochets (identité et coordonnées de l'exploitant, hébergeur, sous-traitants, contact) doivent être complétés. La lecture de la notice est enregistrée à l'inscription et à la création de chaque profil, avec la finalité et la base légale proposée ; ce n'est pas un « consentement RGPD » global.

## À valider avant ouverture publique

1. Identité et coordonnées du responsable de traitement ; hébergement (région UE) et prestataire d'e-mail, contrats de sous-traitance.
2. Bases légales par finalité (compte adulte, suivi pédagogique, sécurité, e-mails transactionnels).
3. Mécanisme d'accord adapté aux moins de 15 ans si une finalité repose sur le consentement (la vérification de l'e-mail adulte ne prouve pas l'autorité parentale).
4. Durées de conservation proposées ci-dessus ; procédure d'incident ; nécessité d'une AIPD.
5. Calibrage d'Argon2id sur le matériel de production (cible 100 à 300 ms par hachage).
6. Recette avec enfants accompagnés et audit d'accessibilité manuel.
