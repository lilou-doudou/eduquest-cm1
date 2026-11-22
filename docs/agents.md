# Équipe EduQuest

Trois agents de projet sont définis sous `.codex/agents/`. Leur rôle est persistant ;
leurs processus démarrent à la demande et ne tournent pas en permanence.
Le fil principal coordonne les trois agents sans être un quatrième spécialiste.
Les modèles et permissions sont hérités de la session ; ces fichiers ne leur donnent
aucun accès supplémentaire.

| Agent | Mission | Sortie attendue |
|---|---|---|
| `qa` | Planifier les tests et écrire les vérifications du comportement attendu | Critères, tests et preuves |
| `developpeur` | Implémenter en Java/React et résoudre les échecs réels | Diff et vérifications |
| `reviewer_integrateur` | Relire indépendamment et préparer l'intégration | Findings et verdict |

## Démarrer une tâche

Dans une conversation Codex ouverte sur ce projet, utiliser par exemple :

> Utilise les agents qa, developpeur et reviewer_integrateur pour [objectif].
> Le QA définit les critères et les tests, le développeur implémente, puis le reviewer
> relit indépendamment. Attribue des fichiers exclusifs, attends leurs résultats et
> rends les commandes exécutées, les résultats et les limites.

Les agents personnalisés suivent le format documenté par
[OpenAI Docs](https://learn.chatgpt.com/docs/agent-configuration/subagents).
Si une session déjà ouverte ne découvre pas les nouvelles définitions, démarrer
une nouvelle conversation sur le projet. Dans la session de mise en place, les trois
rôles sont lancés explicitement avec ces mêmes responsabilités.

## Cycle d'une tâche

1. Le coordinateur fixe l'objectif, les critères d'acceptation, le périmètre de chaque
   agent et le propriétaire des services de test.
2. Le QA identifie les risques et écrit les tests pertinents. Le développeur peut
   analyser les chemins concernés pendant cette préparation.
3. Le développeur implémente, exécute les tests et remet le diff au reviewer.
4. Le reviewer inspecte le résultat indépendamment. Les défauts reviennent au
   développeur ; les lacunes de couverture reviennent au QA.
5. Le résultat combiné est vérifié. Si la mission inclut une fusion de branches,
   seul l'intégrateur la réalise après résolution des P1/P2 et réussite des contrôles.
   La publication et le déploiement restent des actions distinctes.

Un agent peut préparer une autre tâche pendant une étape indépendante. Une revue
finale doit porter sur le diff final, après les corrections.

## Isolation

Pour plusieurs tâches qui écrivent en parallèle, utiliser des worktrees distincts
et une branche par tâche. La création de worktrees dans Codex est décrite dans la
[documentation officielle](https://learn.chatgpt.com/docs/environments/git-worktrees).
Intégrer ensuite les commits nécessaires dans une seule branche et relancer les
contrôles sur le résultat combiné.

Pour une petite tâche commune dans un checkout partagé, le coordinateur peut
attribuer des fichiers exclusifs, par exemple : QA sur un nouveau test, développeur
sur l'application, reviewer en lecture seule. Si un fichier partagé doit changer,
transférer sa responsabilité explicitement avant de le modifier.

Les worktrees ne suffisent pas pour isoler les services. Les ports E2E actuels sont
5433 (PostgreSQL), 8081 (API) et 5174 (frontend). Un seul agent lance la pile actuelle.
Si le port PostgreSQL est occupé, utiliser par exemple
`E2E_POSTGRES_PORT=55433 make test-e2e` (le défaut reste 5433).
Mailpit peut être partagé avec le développement : créer des adresses uniques et ne
pas purger la boîte globale. Ne jamais effacer un volume ou une base existants pour
libérer un test.

## Première mission

Validation des correctifs de revue : consommation concurrente d'un jeton de compte,
isolation du cache entre profils, conservation de la correction après panne réseau,
et présence des en-têtes de sécurité nginx. Les preuves et limites sont consignées
dans [le rapport de validation](validation-agents.md).
