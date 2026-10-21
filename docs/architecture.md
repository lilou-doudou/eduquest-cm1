# Architecture

## Vue d'ensemble

```text
Navigateur ──HTTPS──► reverse proxy TLS ──► nginx (bundle React, /api relayé) ──► Spring Boot :8080 ──► PostgreSQL 16
                                                                                    └──► SMTP (Mailpit en local)
```

En développement, Vite (`:5173`) sert le frontend et relaie `/api` vers Spring Boot (`:8080`) : même origine, aucune règle CORS. Le serveur est la seule autorité pour l'ordre des questions, les corrections, les déblocages, l'XP et les récompenses ; le client n'envoie jamais de score.

## Monorepo et versions figées

| Partie | Choix | Verrouillage |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Hibernate 7, Spring Security 7, Spring Session JDBC, Flyway, springdoc 3.1.1, BouncyCastle 1.86 (Argon2id) | `pom.xml` + wrapper Maven 3.9.11 |
| Frontend | React 19, TypeScript 6, Vite 8, React Router 7, TanStack Query 5, three 0.186, @react-three/fiber 9 | `package-lock.json`, Node 22 (`.nvmrc`) |
| E2E | @playwright/test 1.56.1, @axe-core/playwright 4 | `e2e/package-lock.json` |
| Base | PostgreSQL 16 (image `postgres:16-alpine`) | `compose.yaml` |

Spring Boot 4.1.1 est la version stable courante au démarrage du projet ; elle implique Jackson 3 (`tools.jackson.*`).

## Backend : monolithe modulaire

Package racine `fr.eduquest`, un sous-package par module ; chaque module suit controllers → services transactionnels → repositories, avec des DTO (records) distincts des entités JPA.

| Module | Responsabilités principales |
|---|---|
| `auth` | Compte adulte, e-mails de vérification et de réinitialisation (jetons stockés en SHA-256), connexion, réauthentification, bascule parent ↔ enfant |
| `family` | Profils enfants (5 maximum, contrôlé sous verrou), avatars, données de démonstration du profil dev |
| `catalog` | Lecture des mondes/niveaux/missions, règles de déblocage, import CLI des contenus versionnés |
| `learning` | Parties, normalisation et correction des réponses, indices, clôture, progression et indicateurs de compétence |
| `rewards` | Évaluation des règles de badges et accessoires, équipement |
| `privacy` | Notice versionnée, historique des informations délivrées, export, suppression, purges planifiées |
| `shared` | Sécurité, sessions, CSRF, erreurs problem+json, rate limit persistant, audit, horloge injectable, verrous |

### Sessions et contexte

- Session opaque côté serveur (Spring Session JDBC), cookie `EQSESSION` HttpOnly, SameSite=Lax, Secure hors profil dev.
- Le principal de session (`SessionPrincipal`) porte le rôle `PARENT` ou `CHILD`, l'identifiant du parent, l'enfant éventuel, l'heure de dernière authentification et le début de session. Son nom est l'identifiant du parent, ce qui permet de révoquer toutes les sessions d'une famille (`FindByIndexNameSessionRepository`).
- Chaque changement de privilège (connexion, passage en mode enfant, retour adulte) invalide l'ancienne session et en crée une nouvelle ; le jeton CSRF est renouvelé avec elle.
- Inactivité : 30 min (adulte), 60 min (enfant). Durée absolue de 12 h appliquée par `AbsoluteSessionTimeoutFilter` avec l'horloge injectable.
- Les routes d'apprentissage utilisent toujours l'enfant de la session, jamais un identifiant reçu dans le corps.

### Autorisations (`SecurityConfig`)

| Routes `/api/v1` | Accès |
|---|---|
| `GET /auth/csrf`, `GET /privacy/notice`, `POST /auth/{register,verify-email,login,forgot-password,reset-password}` | Public (CSRF exigé sur les POST) |
| `/auth/me`, `/auth/logout` | Toute session |
| `/children/**`, `/privacy/**`, `/account`, `/auth/child-session`, `/auth/reauthenticate` | `PARENT` |
| `/worlds/**`, `/me/**`, `/missions/**`, `/attempts/**`, `/auth/parent-session` | `CHILD` |
| Tout le reste | Refusé |

Les ressources d'une autre famille renvoient 404 (vérification de propriété dans les services). La liste complète des opérations est dans [`api/openapi.yaml`](api/openapi.yaml), généré depuis le code (`make openapi`).

### Intégrité et concurrence

- Contraintes en base : clés étrangères, unicité `(child_id, reward_id)`, `(attempt_id, exercise_id)`, `xp_event.attempt_id`, index partiels uniques pour la partie `IN_PROGRESS` par enfant et mission et pour la version `PUBLISHED` par mission, `CHECK` sur les bornes (étoiles 0..3, XP 0..70…).
- Verrous pessimistes (`SELECT … FOR UPDATE` via `Locks`) toujours dans l'ordre **parent → enfant → partie**, puis progression ; la clôture revérifie l'état après acquisition. Les parties portent aussi un `@Version` optimiste.
- La clôture, la mise à jour de `mission_progress`, l'écriture de `xp_event` et l'attribution des récompenses (`INSERT … ON CONFLICT DO NOTHING`) se font dans une seule transaction. Le total XP est la somme de `mission_progress.xp_awarded` ; l'invariant « égal à la somme de `xp_event` » est vérifié par les tests d'intégration.
- Idempotence : rejouer la même réponse renvoie la même correction ; une réponse différente après validation renvoie 409 ; rejouer une clôture renvoie le bilan mémorisé (`completion_summary`).

### Modèle de données

Le schéma (`backend/src/main/resources/db/migration/V1__schema.sql`) suit le §10 du cahier des charges, avec ces ajouts :

| Ajout | Raison |
|---|---|
| `parent_account.last_activity_at`, `inactivity_notified_at` | Purge après 12 mois d'inactivité avec préavis de 30 jours |
| `world.description`, `exercise.instruction`, `reward_definition.description/slot/rule_type` | Contenus affichés et règles typées |
| `mission_version.content_hash` | Import idempotent : une version n'est créée que si le contenu change |
| `attempt.completion_summary` (JSONB) | Bilan rejouable à l'identique |
| `rate_limit_bucket` | Rate limit persistant, partagé entre instances |
| `deletion_record` | Registre minimal des suppressions à réappliquer après restauration (purgé à 35 jours) |
| Tables Spring Session | Sessions serveur |

`ddl-auto=validate` : Hibernate vérifie le schéma, Flyway seul le modifie.

## Frontend

```text
src/app         routes, client de requêtes, session, préférences (son, animations, carte 2D)
src/api         client HTTP typé (CSRF, session expirée, erreurs) et types des DTO
src/components  mise en page, garde de rôle, états, étoiles, barres, champs, avatar SVG
src/features    auth, family, worlds, missions, rewards, progress, privacy, settings
src/scene       carte 3D (chargée à la demande, chunk « scene3d ») et carte 2D SVG
```

- Le client HTTP récupère le jeton CSRF, le renvoie sur chaque mutation et réessaie une fois après un `CSRF_INVALID` ; un 401 `SESSION_REQUIRED` redirige vers la connexion avec un message « session expirée ».
- La carte 3D est un composant paresseux : sans WebGL, en cas d'erreur de rendu ou sous 30 images/s (mesurées 3 s après 1 s de chauffe), la carte 2D SVG prend le relais. La liste HTML des mondes est toujours présente sous la carte.
- Préférences locales non sensibles uniquement (`localStorage`) : son (désactivé par défaut), animations réduites, carte 2D forcée.

## Contenus

Les fichiers JSON de `content/` sont validés (schémas `content/schema/*.schema.json` + cohérence croisée) puis importés par un outil CLI Spring (`--eduquest.content.import=true`). Voir [content-authoring.md](content-authoring.md).
