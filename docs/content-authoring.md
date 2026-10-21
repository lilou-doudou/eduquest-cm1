# Écrire et versionner les contenus

Les contenus pédagogiques sont des fichiers JSON versionnés dans `content/`. Ils sont validés hors base (`make content-validate`) puis importés par l'outil CLI du backend (`make content-import`). Aucun corrigé n'est copié dans le frontend.

```text
content/
  schema/                 schémas JSON (draft 2020-12)
  worlds/worlds.json      6 mondes, niveaux des mondes jouables, compétences
  maths/maths-N-M.json    mission M du niveau N des Îles des Nombres
  english/english-N-M.json
  geometrie/geometrie-N-M.json Vallée des Formes
  geographie/geographie-N-M.json Archipel des Explorateurs
  sciences/sciences-N-M.json   Forêt du Vivant
  physique/physique-N-M.json   Cité des Inventeurs
  rewards/rewards.json    badges et accessoires
  assets-manifest.json    inventaire des ressources visuelles et licences
```

## Format d'une mission

```json
{
  "schemaVersion": 1,
  "code": "MATHS-1-1", "world": "iles-des-nombres", "level": 1, "ordinal": 1,
  "title": "Les quais du port",
  "exercises": [
    {
      "skill": "MATH_ADD_SUB", "type": "NUMBER", "difficulty": 1,
      "instruction": "Calcule.",
      "prompt": "245 + 132",
      "answer": { "number": "377" },
      "hint": "Additionne d'abord les unités, puis les dizaines, puis les centaines.",
      "explanation": "245 + 132 = 377."
    }
  ]
}
```

| Champ | Règle |
|---|---|
| `code` | Identité stable de la mission (`MATHS-1-1`) : ne jamais la changer, l'XP et la progression s'y rattachent |
| `world`, `level`, `ordinal` | Monde jouable existant, niveau 1–3, mission 1–2 ; position unique |
| `exercises` | Exactement 5 |
| `skill` | Code de `worlds.json` de la même matière |
| `type` | `MCQ` (avec `options` et `answer.optionId`), `NUMBER` (`answer.number`, `unit` facultatif), `TEXT` (`answer.accepted`) |
| `options` | 2 à 4 options (3 dans le corpus actuel), identifiants `a`–`d` stables, libellés distincts, une seule bonne réponse |
| `answer.number` | Décimal en chaîne (`"12,5"` ou `"12.5"`) ; comparé exactement après normalisation |
| `answer.accepted` | Liste fermée des variantes acceptées (casse, espaces, apostrophes et ponctuation finale normalisés automatiquement ; rien d'autre) |
| `difficulty` | 1 à 3 |
| Textes | Consigne, énoncé, indice et explication obligatoires, texte brut (pas de HTML), aucune question sur la vie privée de l'enfant |

## Contrôles

`make content-validate` (script `frontend/scripts/validate-content.mjs`) vérifie les schémas puis : références monde/niveau/compétence, compétence de la bonne matière, bonne réponse parmi les options, doublons (codes, positions, options, variantes), assets présents dans le manifeste, et 6 missions de 5 exercices par monde jouable (36 missions / 180 exercices aujourd'hui). L'importeur backend (`ContentImporter`) refait ces contrôles et refuse tout le lot avant la moindre écriture en cas d'erreur (`invalidContentIsRejectedBeforeAnyWrite`).

## Versionnement

- L'import calcule une empreinte de chaque mission. Contenu inchangé → rien n'est écrit (réimport sans doublon, testé).
- Contenu modifié → nouvelle `mission_version` `PUBLISHED`, l'ancienne passe `RETIRED`. Une version publiée n'est jamais modifiée.
- Les parties déjà commencées restent attachées à leur version ; les nouvelles parties utilisent la nouvelle. L'XP reste plafonné par mission (`code`), pas par version : republier ne permet pas de regagner de l'XP (`newVersionDoesNotAlterAttemptInProgressNorXpCeiling`).
- Un changement pédagogique majeur doit être une nouvelle mission (nouveau `code`), hors MVP.

## Procédure

1. Modifier ou ajouter le fichier JSON.
2. `make content-validate`.
3. `make content-import` sur la base de développement, puis jouer la mission.
4. Faire relire par un enseignant (exactitude, niveau CM1, variantes acceptées).
5. Commit ; en production, lancer l'import avec l'image backend (voir [deployment.md](deployment.md)).
