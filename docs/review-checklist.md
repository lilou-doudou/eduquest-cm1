# Checklist de revue (§21) avec pointeurs de preuve

Pour chaque point, l'état déclaré par l'équipe de réalisation et l'endroit où le vérifier. La revue indépendante doit reproduire ces preuves et ne pas s'y fier. Légende : ✅ implémenté et testé, 🟡 partiel ou à valider, ❌ non fait. Les tests cités sont dans `backend/src/test/java/fr/eduquest/it/*IntegrationTest.java` (PostgreSQL réel via Testcontainers), `frontend/tests/` et `e2e/tests/`.

## Fonctionnel et pédagogie

| | Point | Preuve |
|---|---|---|
| ✅ | Deux mondes jouables, six visibles ; les quatre futurs sont inaccessibles côté API | `MissionFlow.comingSoonWorldsAreVisibleButNotPlayable`, `e2e/no-webgl.spec.ts` |
| 🟡 | Douze missions et soixante vrais exercices, bonne répartition et corrections relues | `make content-validate` ; corpus détaillé dans `docs/pedagogy.md`. **Relecture enseignant non faite.** |
| ✅ | Progression, indices, reprise, étoiles, XP et badges conformes aux exemples | `MissionFlow.*` (3/5 → 1★ +50 XP, 2/5 → +20 sans déblocage, reprise à la question 3, indice idempotent), `ScoringRulesTest`, `e2e/parcours-famille.spec.ts` |
| ✅ | Rang d'aventurier distinct des niveaux pédagogiques ; récompense à chaque rang atteint | `ScoringRules.adventurerLevel`, `content/rewards/rewards.json` (rangs 2–9), `RewardService` |
| ✅ | Rejouer ne crée pas de gain illimité ; une amélioration attribue seulement le delta | `MissionFlow.replayingGivesOnlyTheImprovementAndNoDuplicateBadge` |
| ✅ | Les indicateurs de compétence affichent effectifs et recours aux indices | `ProgressService.skillIndicators`, `SkillStatusTest`, écran `ParentProgressPage` |

## Sécurité et confidentialité

| | Point | Preuve |
|---|---|---|
| ✅ | Toutes les routes ont une politique d'autorisation ; tests inter-familles probants | `SecurityConfig` (refus par défaut), `Isolation.otherFamilyGets404OnEveryRouteCarryingAnIdentifier` |
| ✅ | Session enfant incapable d'accéder aux routes adultes ; retour parent réauthentifié | `Isolation.childSessionCannotReachParentRoutesEvenBypassingTheFrontend`, `returnToParentRequiresOwnerCredentials`, E2E parcours famille |
| ✅ | Cookies, rotation, expiration absolue, CSRF, récupération et rate limit vérifiés | `AuthAndSecurity.*` (10 tests) |
| ✅ | Aucune solution dans le bundle/catalogue ; correction uniquement après soumission autorisée | `ContentVersioning.catalogNeverExposesAnswerKeys`, `frontend/tests` (contrat des DTO), `MissionFlow.bypassAttemptsAreRefused` |
| 🟡 | Réponses, mots de passe, jetons et données personnelles exclus des logs | Aucune journalisation de corps/jetons dans le code (revue) ; pas de test automatisé dédié |
| ✅ | Export complet sans secrets ; suppression active et traitement des sauvegardes documentés | `Privacy.exportContainsFamilyDataButNoSecret`, `accountDeletionRemovesEverythingOfThatFamilyOnly`, `docs/deployment.md` |
| ✅ | Purges réellement exécutables et testées ; aucune donnée enfant superflue | `Privacy.inactiveFamiliesAreNotifiedThenDeleted`, `reconnectionCancelsInactivityDeletion`, `neverVerifiedAccountsArePurgedAfterSevenDays` (horloge contrôlée) |
| 🟡 | Bases légales et information mineurs cadrées ; absence de prétendue conformité automatique | `docs/security-privacy.md` ; notices en **brouillon** avec champs à compléter |
| ✅ | Aucun compte démo ni secret local utilisable en production | `ProductionGuard` + `ProductionGuardTest`, vérifié avec l'image Docker ; seed limité au profil `dev` |

## Intégrité et architecture

| | Point | Preuve |
|---|---|---|
| ✅ | Migrations sur base vierge ; contraintes et index présents, pas seulement des validations UI | `V1__schema.sql`, chaque test d'intégration part d'une base migrée, job CI `fresh-clone` |
| ✅ | Versions publiées immuables, historique conservé, réimport idempotent | `ContentVersioning.reimportCreatesNoDuplicate`, `newVersionDoesNotAlterAttemptInProgressNorXpCeiling` |
| ✅ | Clôture et récompenses transactionnelles ; concurrence et doubles envois testés | `Concurrency.*`, `MissionFlow.lostNetworkResponsesCanBeReplayedWithoutExtraGain` |
| ✅ | Verrous pris dans un ordre cohérent ; aucun total XP divergent entre tables | `shared/Locks.java` (parent → enfant → partie), test de somme `xp_event` = `mission_progress.xp_awarded` |
| 🟡 | API et DTO typés conformes à OpenAPI ; erreurs non sensibles | `docs/api/openapi.yaml` généré depuis le code ; les corps JSON libres (`answer`, export) y sont décrits comme objets génériques |
| ✅ | Pas de surarchitecture ni dépendance à un service payant pour le local | Monolithe unique, Compose local |

## Interface et livraison

| | Point | Preuve |
|---|---|---|
| 🟡 | Clavier, focus, zoom, contrastes, lecteurs d'écran et animations réduites vérifiés | axe-core WCAG 2.1 AA sans violation sur 10 écrans, mission au clavier seul, animations réduites (`e2e/accessibilite.spec.ts`). **Zoom 200 % et lecteur d'écran non vérifiés manuellement.** |
| ✅ | Mode sans WebGL complet, états réseau et session expirée gérés | `e2e/no-webgl.spec.ts`, `frontend/tests/client.test.ts`, `play.test.tsx` |
| ✅ | Démarrage neuf reproductible avec les commandes documentées | README ; `make content-import` vérifié ; job CI `fresh-clone` (non exécuté sur GitHub à ce jour) |
| ✅ | Tests réellement exécutés, preuves de résultats et limites explicites | `docs/rapport-mvp.md` |
| 🟡 | Assets locaux licenciés, dépendances verrouillées et CI utilisable | `docs/assets-licenses.md`, lockfiles ; licence du dépôt non choisie ; CI écrite mais jamais exécutée sur GitHub |
| ✅ | Aucune fonction simplement simulée présentée comme terminée | Toutes les fonctions décrites passent par PostgreSQL ; le rapport liste ce qui manque |
