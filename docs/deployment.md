# Déploiement et exploitation

**Aucun déploiement public n'a été réalisé ni n'est autorisé sans décision explicite du porteur du projet.** Ce document décrit la cible et les procédures à préparer ; les choix d'hébergeur, de prestataire e-mail et de région restent à faire (UE recommandée).

## Cible

```text
Internet ─► reverse proxy TLS (Caddy/Traefik/nginx, HSTS) ─► web (nginx non root, :8081)
                                                               ├─ /        bundle React
                                                               └─ /api/    ─► backend (Spring Boot, profil prod, :8080)
                                                                                └─► PostgreSQL 16 (réseau privé)
```

- Frontend et API servis sous **la même origine** ; aucun CORS.
- `/actuator/*` et `/v3/*` ne sont pas relayés par nginx (404) ; la sonde de santé interroge le backend directement sur le réseau privé.
- PostgreSQL n'est jamais exposé publiquement ; l'utilisateur applicatif n'a que les droits sur sa base. Mailpit n'existe pas en production.

## Images

| Image | Fichier | Détails |
|---|---|---|
| `eduquest-backend` | `infra/backend.Dockerfile` | Multi-étapes (JDK 21 → JRE 21 Alpine), utilisateur `eduquest` non root, profil `prod`, healthcheck sur `/actuator/health/readiness`, contenus copiés dans `/app/content` pour l'import |
| `eduquest-frontend` | `infra/frontend.Dockerfile` + `infra/nginx.conf` | Multi-étapes (Node 22 → nginx-unprivileged), UID 101, en-têtes de sécurité, cache immuable des assets fingerprintés |

```sh
docker build -f infra/backend.Dockerfile -t eduquest-backend .
docker build -f infra/frontend.Dockerfile -t eduquest-frontend .
```

Un exemple d'assemblage est fourni dans `infra/compose.prod.example.yaml` (à placer derrière le reverse proxy TLS).

## Configuration de production

Variables à fournir par un gestionnaire de secrets ou un fichier hors dépôt : `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_BASE_URL` (HTTPS), `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, `SPRING_PROFILES_ACTIVE=prod`.

Le profil `prod` active cookie `Secure`, HSTS, `forward-headers-strategy: native` (le proxy doit transmettre `X-Forwarded-Proto`/`For`) et désactive Swagger. La garde `ProductionGuard` refuse de démarrer si le mot de passe de base est vide ou de démonstration, si `APP_BASE_URL` n'est pas en HTTPS ou si les données de démonstration sont activées.

Si plusieurs familles partagent une IP publique (école, NAT), ajuster `EDUQUEST_RATE_LIMIT_IP_FACTOR`. Recalibrer Argon2id (`EDUQUEST_SECURITY_ARGON2_MEMORY_KIB`, `EDUQUEST_SECURITY_ARGON2_ITERATIONS`) pour viser 100 à 300 ms par hachage sur le matériel cible.

## Migrations et contenus

1. Sauvegarder la base.
2. Démarrer la nouvelle image backend : Flyway applique les migrations au démarrage (`ddl-auto=validate` ensuite). Les migrations sont additives ; une migration destructive exige une stratégie écrite (copie, double écriture, bascule) validée avant livraison.
3. Importer les contenus (idempotent) :
   ```sh
   docker run --rm --env-file prod.env eduquest-backend \
     --spring.main.web-application-type=none --eduquest.content.import=true
   ```
4. Vérifier `/actuator/health/readiness` puis basculer le trafic.

Revenir à une ancienne image **n'annule pas** une migration : le retour arrière passe par une restauration de sauvegarde ou une migration corrective.

## Sauvegardes et restauration

- Sauvegardes chiffrées (par exemple `pg_dump` chiffré ou snapshots du service géré), rotation de 30 jours maximum, stockées hors de l'hôte.
- Restauration testée au moins une fois par trimestre sur un environnement isolé.
- **Après toute restauration**, réappliquer les suppressions enregistrées depuis la date de la sauvegarde : pour chaque ligne de `deletion_record` (identifiant de compte supprimé, date) de la base courante, supprimer ce compte dans la base restaurée avant de rouvrir le service. Le registre est conservé 35 jours (au-delà de la rotation des sauvegardes) puis purgé.

## Exploitation courante

| Sujet | Procédure |
|---|---|
| Purges | Tâche interne quotidienne à 03:30 UTC (`eduquest.retention.cron`) ; surveiller ses journaux. Avec plusieurs instances, n'en activer qu'une (ou ajouter un verrou partagé). |
| Rotation des secrets | Changer le mot de passe PostgreSQL et SMTP dans le gestionnaire de secrets, redémarrer les instances. Toutes les sessions peuvent être révoquées en vidant les tables `spring_session*`. |
| Mises à jour | Dépendances mises à jour par PR (CI : tests, audit npm, revue des dépendances) ; images reconstruites et redéployées. |
| Incident de sécurité | Contenir (couper l'accès, révoquer les sessions), préserver les journaux, évaluer l'impact sur les données d'enfants, notifier l'autorité de contrôle dans les 72 h si requis et informer les familles concernées ; procédure à formaliser par l'exploitant. |
| Surveillance | `/actuator/health` (liveness/readiness), journaux applicatifs sans données personnelles en clair, `traceId` dans chaque erreur pour la corrélation. |

## Performance mesurée

Mesure locale reproductible : `make perf` (20 enfants simultanés jouant chacun 3 missions complètes sans temps de réflexion, sur la pile isolée). Résultat du 21/10/2026 sur 4 vCPU Xeon 2,8 GHz, 15,7 Go, backend et PostgreSQL sur la même machine : 800 requêtes, **p95 293 ms**, p50 150 ms, max 522 ms (objectif p95 < 500 ms atteint). À refaire sur l'hébergement réel.
