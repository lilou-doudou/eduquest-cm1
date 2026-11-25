# Première mission des trois agents — 23 octobre 2026

## Périmètre

Mettre en place les rôles QA, développeur Java/React et reviewer-intégrateur, puis
valider les quatre correctifs de revue sur le code actuel. Le point de départ est
le commit `ba06f2d` ; le dépôt était propre avant cette mission.

Cette première mission utilise un checkout partagé avec attribution exclusive :
QA sur `e2e/tests/regressions-revue.spec.ts`, développeur sur l'exécution E2E et les
éventuels correctifs nécessaires, reviewer en lecture seule, coordinateur sur la
configuration et la documentation. Aucune branche n'a été fusionnée.

## Livrables

- Trois définitions de projet `.codex/agents/*.toml`, sans modèle ni permissions supplémentaires.
- Règles communes `AGENTS.md` et guide `docs/agents.md`.
- Quatre scénarios Playwright : confirmation concurrente, reset concurrent avec
  vérification du mot de passe gagnant, coupure réseau réelle et reprise,
  changement de profil sans rechargement du document.
- Port PostgreSQL E2E configurable par `E2E_POSTGRES_PORT`, défaut 5433.
- Mise à jour du scénario sans WebGL pour le catalogue actuel : six mondes jouables,
  navigation au clavier et verrouillage des missions conservés.

## Résultats

| Contrôle | Résultat |
|---|---|
| Syntaxe des trois définitions TOML | Réussi avec le parseur TOML Python, champs obligatoires présents et noms cohérents |
| `cd frontend && npm run lint` | Réussi |
| `cd frontend && npm test` | 18 tests réussis, 4 fichiers |
| `cd e2e && npx tsc -p .` | Réussi |
| nginx sous Docker | En-têtes de sécurité présents sur HTML, route SPA, JS et asset inexistant (404) |
| Première suite E2E | 11 réussis, 3 ignorés, 1 échec : ancien scénario de monde futur, corrigé par le QA |
| Suite E2E après actualisation | **12 réussis**, 3 captures ignorées car `CAPTURE` absent ; code de sortie 0 |
| Build frontend de production (via lancement E2E) | Réussi |
| `bash -n scripts/e2e.sh` et `git diff --check` | Réussis |

Le reviewer indépendant n'a pas identifié de bloqueur dans les quatre correctifs,
les tests ajoutés ou les définitions des trois rôles.

Commande finale exécutée par le développeur, après compilation du jar :

```sh
E2E_POSTGRES_PORT=55433 SKIP_BACKEND_BUILD=1 \
  PLAYWRIGHT_BROWSERS_PATH=/private/tmp/eduquest-playwright make test-e2e
```

Le rapport HTML local est dans `e2e/playwright-report/index.html` (généré, ignoré
par Git). Les quatre nouveaux scénarios et le scénario sans WebGL réussissent.
La base E2E est arrêtée après les tests, le conteneur nginx éphémère a été supprimé
et les services de développement sont inchangés. Les artefacts suivis générés par
TypeScript ont été restaurés.

Verdict : **validation réussie**, aucun bloqueur relevé par le reviewer indépendant.
Les définitions TOML sont analysées et relues ; leur découverte automatique par une
future session Codex n'a pas été testée avec un nouveau processus agent. Cette
mission a effectivement lancé les trois rôles par délégation explicite.

## Conditions d'exécution et limites

- Le port 5433 était occupé. La pile de développement a été préservée ; la pile E2E
  utilise le port PostgreSQL 55433 et une base jetable distincte.
- Les dépendances locales absentes/incomplètes ont été réinstallées depuis les lockfiles.
- Le frontend E2E est servi par Vite : il ne permet pas de conclure sur nginx. Les
  en-têtes nginx ont été vérifiés séparément dans un conteneur éphémère.
- Le scénario de changement de profil vérifie la progression ; la purge des
  récompenses est couverte par le test React existant.
- Le test sans WebGL conservait un dernier scénario de monde futur alors que le
  catalogue et `MissionFlowIntegrationTest.allSixWorldsArePlayable` définissent
  désormais six mondes jouables. Le QA a aligné ce scénario sur le contrat actuel,
  sans modifier le catalogue ni les règles de déblocage.
- Les tests React émettent un avertissement jsdom sur le canvas ; les assertions réussissent.
- Aucun déploiement, push ou merge n'est réalisé par cette mission.
