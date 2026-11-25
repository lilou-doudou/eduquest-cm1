# EduQuest — règles de travail

## Projet

Monorepo éducatif : backend Java 25 / Spring Boot, frontend React / TypeScript,
contenus JSON versionnés, PostgreSQL et tests Playwright. Lire README.md pour le
démarrage et docs/architecture.md pour les contrats et invariants.

## Équipe de trois agents

Quand l'utilisateur demande un travail en équipe, utiliser les rôles définis dans
`.codex/agents/` : `qa`, `developpeur` et `reviewer_integrateur`.
Le fil principal coordonne la tâche, attribue les fichiers et rassemble les résultats.
Suivre docs/agents.md : critères → tests → implémentation → revue → validation.
Ne pas lancer d'agents supplémentaires ni confondre les trois rôles avec trois
modifications concurrentes des mêmes fichiers.

## Collaboration et Git

- Lire `git status` avant de travailler ; préserver les changements existants.
- Préférer un worktree distinct pour chaque agent qui écrit sur une tâche indépendante.
  Un worktree n'isole pas les ports, les bases ni les conteneurs Docker.
- Dans un checkout partagé, attribuer des fichiers exclusifs avant toute écriture.
  Arrêter les écritures concurrentes si un même fichier devient nécessaire aux deux agents.
- Un seul agent possède le démarrage/arrêt de la pile E2E à la fois.
- Ne pas fusionner, pousser, déployer ni supprimer des données hors de la mission autorisée.
- L'intégrateur est le seul rôle qui fusionne lorsque la fusion fait partie de la demande.

## Invariants de l'application

- Toute lecture ou mutation privée doit vérifier la famille et le rôle côté serveur.
- Le serveur décide des réponses, scores, XP, récompenses et déblocages.
- Conserver rotation des sessions, CSRF et purge du cache personnel lors des changements de profil.
- Préserver l'atomicité, l'idempotence et l'ordre des verrous documenté.
- Les tests utilisent une base jetable ; jamais `reset-dev-db` ni la base de développement.
- Ne pas journaliser mots de passe, jetons ou données personnelles.
- Ne pas modifier une migration déjà appliquée pour corriger un schéma ; ajouter une migration.

## Vérifications

Choisir les contrôles selon les changements et réexécuter ceux touchés par une correction :

- Backend : `cd backend && ./mvnw -B verify` (Docker pour Testcontainers).
- React : `cd frontend && npm run lint && npm run typecheck && npm test`.
- Bundle : `cd frontend && npm run build`.
- Contenus : `make content-validate`.
- E2E : `make test-e2e` ; tests ciblés : `./scripts/env.sh ./scripts/e2e.sh --project=desktop nom-du-test`.
- TypeScript E2E : `cd e2e && npx tsc -p .`.
- nginx : vérifier la syntaxe et les en-têtes HTTP dans son image ; Vite ne valide pas nginx.

Utiliser les lockfiles et la version Node de `.nvmrc`. Ne pas changer de dépendances
sans nécessité. Ne pas désactiver des tests ni affaiblir leurs assertions pour faire passer le code.

## Compte rendu

Préciser le résultat, les fichiers concernés, les commandes réellement exécutées,
leurs résultats et les limites. Ne pas annoncer un contrôle non exécuté comme réussi.
Les findings de revue comportent priorité, fichier/ligne et scénario déclencheur.
