# EduQuest CM1

Jeu éducatif familial pour élèves de CM1 : six mondes visibles sur une carte 3D (avec une carte 2D équivalente), dont deux jouables au MVP, **Les Îles des Nombres** (mathématiques) et **Le Royaume des Mots** (anglais). Chaque monde jouable propose 3 niveaux × 2 missions × 5 questions, soit 60 exercices corrigés côté serveur, avec étoiles, XP, rangs d'aventurier, badges et accessoires d'avatar. Un adulte crée le compte familial, gère jusqu'à cinq profils enfants et suit la progression.

Le cahier des charges est dans [`EDUQUEST_CM1_SPECIFICATIONS.md`](EDUQUEST_CM1_SPECIFICATIONS.md). L'état réel de la livraison, les commandes exécutées et les écarts sont dans [`docs/rapport-mvp.md`](docs/rapport-mvp.md).

| | |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8, React Router 7, TanStack Query 5, Three.js 0.186 via @react-three/fiber 9 |
| Backend | Java 21, Spring Boot 4.1.1 (Web MVC, Security 7, Data JPA / Hibernate 7, Session JDBC, Validation, Mail), Flyway, Argon2id |
| Base | PostgreSQL 16 |
| Tests | JUnit 5 + Testcontainers PostgreSQL, Vitest + Testing Library, Playwright + axe-core |

## Prérequis

- Java 21 (le wrapper `backend/mvnw` télécharge Maven 3.9.11)
- Node.js 22 LTS (version figée dans `.nvmrc`) et npm (lockfiles fournis)
- Docker avec Docker Compose v2
- GNU Make et curl

Aucun service cloud payant n'est nécessaire.

## Le plus simple : tout en conteneurs (Docker seulement)

Sans Java ni Node, sur Mac (Intel ou Apple Silicon), Linux ou Windows :

```sh
cp .env.example .env
docker compose --profile app up --build
```

Puis http://localhost:3000 (e-mails sur http://localhost:8025). Guide détaillé pour débutant : [docs/guide-demarrage-mac.md](docs/guide-demarrage-mac.md).

## Démarrage rapide pour développer

```sh
cp .env.example .env
make dev
```

`make dev` démarre PostgreSQL et Mailpit (en attendant leurs healthchecks), applique les migrations Flyway, importe les contenus, puis lance le backend et le frontend. Il ne supprime jamais une base existante. `Ctrl+C` arrête backend et frontend ; `make stop` arrête les conteneurs sans toucher aux volumes.

| Service | URL |
|---|---|
| Application | http://localhost:5173 |
| API | http://localhost:8080 (`/actuator/health`) |
| Swagger UI (profil dev uniquement) | http://localhost:8080/swagger-ui.html |
| Boîte e-mail locale (Mailpit) | http://localhost:8025 |

### Compte fictif de démonstration

Créé automatiquement **uniquement avec le profil `dev`** (`eduquest.dev-seed=true`), refusé par la garde de production :

- adulte : `parent.demo@eduquest.local` / `demo-eduquest-local`
- enfants : **Nova** et **Tiago**

On peut aussi créer un compte via « Créer un compte » : le lien de confirmation arrive dans Mailpit.

### Mode manuel (terminaux séparés)

```sh
docker compose up -d postgres mailpit
make backend-dev      # processus long : API sur :8080 (migre la base au démarrage)
make content-import   # dans un autre terminal, après la migration
make frontend-dev     # processus long : Vite sur :5173, relais /api vers :8080
```

## Configuration

Les cibles Make chargent `.env` par [`scripts/env.sh`](scripts/env.sh) (`set -a; . .env`) : Maven ne lit pas `.env` tout seul. Spring lit ensuite les variables d'environnement à travers les placeholders de `backend/src/main/resources/application.yml` :

| Variable | Propriété Spring | Rôle |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `spring.datasource.username/password` | Base (et conteneur Compose) |
| `DATABASE_URL` | `spring.datasource.url` | URL JDBC |
| `APP_BASE_URL` | `eduquest.base-url` | Liens des e-mails (HTTPS obligatoire en prod) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.*` | Envoi des e-mails |
| `MAIL_FROM` | `eduquest.mail.from` | Expéditeur |
| `SPRING_PROFILES_ACTIVE` | profils | `dev` en local, `prod` en production |

Le frontend n'utilise aucune variable `VITE_*` : il appelle `/api` sur la même origine (relais Vite en développement, nginx/reverse proxy en production).

## Commandes

```sh
make help              # liste des cibles
make test              # validation des contenus + backend (Testcontainers, Docker requis) + frontend
make test-e2e          # pile isolée jetable (PostgreSQL :5433, API :8081, bundle :5174) + Playwright
make lint              # ESLint, typecheck frontend et E2E, compilation Java
make build             # jar backend + bundle frontend de production
make content-validate  # schémas JSON et cohérence de content/ sans base
make content-import    # import idempotent des contenus dans la base de développement
make perf              # mesure de charge : 20 enfants simultanés (pile isolée)
make screenshots       # captures des écrans dans docs/screenshots
make openapi           # régénère docs/api/openapi.yaml depuis le backend dev
make stop              # arrête les conteneurs, garde les volumes
make reset-dev-db      # DESTRUCTIF : supprime la base de dev (confirmation « SUPPRIMER »)
```

`make test-e2e` n'utilise jamais la base de développement : il démarre le service Compose `postgres-e2e` (stockage en mémoire, vidé à l'arrêt). Les e-mails de test passent par le même Mailpit, avec des adresses uniques.

## Structure

```text
frontend/   React (src/{app,api,components,features,scene,styles}, tests Vitest)
backend/    Spring Boot (fr.eduquest.{auth,family,catalog,learning,rewards,privacy,shared}), migrations Flyway
content/    mondes, compétences, 12 missions, récompenses, inventaire des assets, schémas JSON
e2e/        Playwright (parcours complet, mobile, sans WebGL, 3D, accessibilité)
infra/      Dockerfiles, nginx, exemple de déploiement
docs/       architecture, OpenAPI, pédagogie, sécurité, contenus, licences, déploiement, revue, rapport
scripts/    démarrage, import, E2E, mesure de charge
```

## Documentation

- [Architecture](docs/architecture.md) et [contrat OpenAPI](docs/api/openapi.yaml)
- [Règles pédagogiques et de score](docs/pedagogy.md)
- [Sécurité et données personnelles](docs/security-privacy.md)
- [Écrire et versionner les contenus](docs/content-authoring.md)
- [Licences des ressources](docs/assets-licenses.md)
- [Déploiement et exploitation](docs/deployment.md)
- [Checklist de revue](docs/review-checklist.md)
- [Rapport de livraison du MVP](docs/rapport-mvp.md) et [captures d'écran](docs/screenshots/)

Aucun déploiement public n'est prévu sans décision explicite du porteur du projet.
