# Règles pédagogiques et de score

Ce document décrit les règles réellement implémentées (`backend/src/main/java/fr/eduquest/learning`, `catalog/UnlockRules.java`, `rewards/RewardService.java`). **Le corpus n'a pas encore été relu par un enseignant de CM1** : cette validation est requise avant toute ouverture publique.

## Structure

- 6 mondes, tous jouables : **Les Îles des Nombres** (mathématiques), **Le Royaume des Mots** (anglais), **La Vallée des Formes** (géométrie), **L'Archipel des Explorateurs** (géographie), **La Forêt du Vivant** (sciences) et **La Cité des Inventeurs** (physique et technologie). Un futur monde marqué `COMING_SOON` afficherait un aperçu ; l'API refuse d'y lancer une partie (`WORLD_NOT_AVAILABLE`).
- Un monde jouable = 3 niveaux ordonnés × 2 missions × 5 exercices, soit 30 exercices par matière, 10 par niveau.
- Chaque exercice a une compétence principale, un type (`MCQ`, `NUMBER`, `TEXT`), une difficulté 1 à 3 (distincte du rang d'aventurier), une consigne, un énoncé, une réponse privée, un indice et une explication.

## Corpus du MVP

| Mission | Titre | Compétences | Types | Difficultés |
|---|---|---|---|---|
| MATHS-1-1 | Les quais du port | Numération ×3, Addition/soustraction ×2 | Nombre ×4, QCM ×1 | 1, 1, 1, 1, 2 |
| MATHS-1-2 | Le phare des calculs | Addition/soustraction ×4, Numération ×1 | Nombre ×4, QCM ×1 | 1, 1, 1, 2, 2 |
| MATHS-2-1 | Le sentier des tables | Multiplication ×5 | Nombre ×4, QCM ×1 | 1, 1, 1, 2, 1 |
| MATHS-2-2 | La clairière du partage | Partage exact ×4, Multiplication ×1 | Nombre ×4, QCM ×1 | 1, 1, 1, 2, 2 |
| MATHS-3-1 | Le chemin des énigmes | Problèmes ×5 | Nombre ×4, QCM ×1 | 1, 1, 1, 1, 2 |
| MATHS-3-2 | Le trésor du sommet | Problèmes ×5 | Nombre ×4, QCM ×1 | 2, 2, 2, 3, 3 |
| ENGLISH-1-1 | Hello, le village ! | Salutations ×5 | QCM ×4, Texte ×1 | 1, 1, 1, 2, 2 |
| ENGLISH-1-2 | L'arc-en-ciel | Couleurs ×5 | QCM ×3, Texte ×2 | 1, 1, 1, 2, 2 |
| ENGLISH-2-1 | Compter les moutons | Nombres ×5 | QCM ×2, Texte ×2, Nombre ×1 | 1, 1, 2, 2, 1 |
| ENGLISH-2-2 | Les animaux de la ferme | Animaux ×5 | QCM ×3, Texte ×2 | 1, 1, 2, 1, 2 |
| ENGLISH-3-1 | La salle de classe | La classe ×5 | QCM ×3, Texte ×2 | 1, 1, 1, 2, 2 |
| ENGLISH-3-2 | Les phrases du château | Phrases simples ×5 | QCM ×3, Texte ×2 | 2, 1, 2, 2, 3 |
| GEOMETRIE-1-1 | Le jardin des figures | Figures planes ×5 | QCM ×3, Nombre ×2 | 1, 1, 2, 2, 2 |
| GEOMETRIE-1-2 | Le chemin des angles droits | Angles et droites ×5 | QCM ×3, Nombre ×1, Texte ×1 | 1, 1, 2, 2, 2 |
| GEOMETRIE-2-1 | Le miroir des symétries | Symétrie ×5 | QCM ×3, Nombre ×2 | 1, 1, 2, 2, 3 |
| GEOMETRIE-2-2 | La tour des solides | Solides ×5 | QCM ×3, Nombre ×2 | 1, 1, 2, 2, 2 |
| GEOMETRIE-3-1 | Le tour du belvédère | Périmètre ×5 | QCM ×1, Nombre ×4 | 1, 1, 2, 2, 3 |
| GEOMETRIE-3-2 | La carte au trésor | Repérage ×5 | QCM ×4, Nombre ×1 | 1, 2, 2, 2, 3 |
| GEOGRAPHIE-1-1 | La boussole et la légende | Lire une carte ×5 | QCM ×3, Texte ×1, Nombre ×1 | 1, 1, 2, 1, 2 |
| GEOGRAPHIE-1-2 | Le tour du monde | Le monde ×5 | QCM ×4, Texte ×1 | 1, 1, 2, 2, 2 |
| GEOGRAPHIE-2-1 | Les paysages de France | Paysages ×5 | QCM ×4, Texte ×1 | 1, 1, 2, 2, 2 |
| GEOGRAPHIE-2-2 | La carte de France | La France ×5 | QCM ×5 | 1, 2, 2, 2, 2 |
| GEOGRAPHIE-3-1 | Habiter et travailler | Habiter ×5 | QCM ×4, Texte ×1 | 1, 1, 2, 2, 2 |
| GEOGRAPHIE-3-2 | Consommer en France | Consommer ×5 | QCM ×4, Texte ×1 | 1, 2, 2, 2, 2 |
| SCIENCES-1-1 | Le jardin des graines | Besoins du vivant ×5 | QCM ×4, Texte ×1 | 1, 1, 1, 2, 2 |
| SCIENCES-1-2 | Qui mange quoi ? | Régimes alimentaires ×3, Chaînes alimentaires ×2 | QCM ×4, Texte ×1 | 1, 1, 2, 2, 3 |
| SCIENCES-2-1 | Le grand tri des animaux | Classer le vivant ×5 | QCM ×4, Nombre ×1 | 1, 1, 2, 2, 2 |
| SCIENCES-2-2 | Le cycle de la vie | Cycles de vie ×5 | QCM ×4, Texte ×1 | 1, 2, 1, 2, 2 |
| SCIENCES-3-1 | Le refuge de la santé | Corps et santé ×5 | QCM ×4, Nombre ×1 | 1, 1, 2, 2, 1 |
| SCIENCES-3-2 | Les gardiens de la forêt | Milieux et environnement ×5 | QCM ×4, Texte ×1 | 1, 2, 2, 1, 2 |
| PHYSIQUE-1-1 | Les trois états de l'eau | États de la matière ×5 | QCM ×3, Nombre ×1, Texte ×1 | 1, 1, 2, 2, 3 |
| PHYSIQUE-1-2 | Le laboratoire des mélanges | Mélanges ×5 | QCM ×4, Nombre ×1 | 1, 1, 2, 2, 2 |
| PHYSIQUE-2-1 | Les sources d'énergie | Énergie ×5 | QCM ×4, Texte ×1 | 1, 1, 2, 2, 2 |
| PHYSIQUE-2-2 | Le circuit de la lampe | Électricité ×5 | QCM ×5 | 1, 1, 2, 2, 1 |
| PHYSIQUE-3-1 | La piste des mouvements | Mouvements ×5 | QCM ×3, Nombre ×2 | 1, 1, 2, 2, 3 |
| PHYSIQUE-3-2 | L'atelier des inventions | Objets techniques ×5 | QCM ×4, Nombre ×1 | 1, 2, 2, 3, 2 |

Progression : Maths (1) nombres entiers, additions et soustractions, (2) multiplication et partage exact, (3) problèmes à une ou deux étapes ; Anglais (1) salutations et couleurs, (2) nombres et animaux, (3) objets de la classe et phrases très simples ; Vallée des Formes (1) figures planes, angles et droites, (2) symétrie et solides, (3) périmètre et repérage sur quadrillage ; Archipel des Explorateurs (1) lire une carte, continents et océans, (2) paysages et carte de France, (3) habiter sa commune et consommer en France ; Forêt du Vivant (1) besoins des êtres vivants et régimes alimentaires, (2) classement des animaux et cycles de vie, (3) santé et environnement ; Cité des Inventeurs (1) états de l'eau et mélanges, (2) énergie et électricité, (3) mouvements et objets techniques. Pas de fractions ni d'audio. En sciences, géométrie et géographie, les réponses en texte court sont en français, avec une liste fermée de variantes (par exemple « métamorphose » avec ou sans accent). Aucun exercice ne demande d'information personnelle.

## Correction (serveur uniquement)

| Type | Réponse attendue | Normalisation (`AnswerNormalizer`) |
|---|---|---|
| QCM | `{"optionId":"b"}` | Comparaison de l'identifiant stable, jamais de la position. 3 options par question. |
| Nombre | `{"number":"1 784"}` | Espaces périphériques ignorés, virgule ou point, séparateurs de milliers par espace (y compris insécable) ; comparaison exacte en `BigDecimal` (pas de flottant binaire). L'unité est affichée à part. |
| Texte (anglais) | `{"text":"Thank you"}` | NFC, apostrophes typographiques normalisées, casse ignorée, espaces regroupés, ponctuation finale retirée ; comparaison à une **liste fermée** de variantes écrites par l'auteur. Aucune tolérance orthographique automatique. |

- Réponse vide : 400, la question n'est pas consommée. Réponse de mauvais type ou clés inattendues : 422 (ou 400 si le corps contient autre chose que `answer`, par exemple `score` ou `isCorrect`).
- Une seule réponse évaluée par question et par partie ; seule la question courante peut recevoir une réponse. La même réponse renvoyée (réseau perdu) produit la même correction ; une réponse différente après validation renvoie 409.
- La bonne réponse et l'explication ne sont envoyées qu'après la soumission ; le catalogue et la question publique ne contiennent jamais de corrigé.
- Indice facultatif, avant la réponse, enregistré une seule fois ; **aucune pénalité** au MVP. Le bilan et le suivi parent affichent le nombre de bonnes réponses obtenues avec indice.

## Étoiles et déblocages

| Bonnes réponses sur 5 | 0–2 | 3 | 4 | 5 |
|---|---|---|---|---|
| Étoiles | 0 | 1 | 2 | 3 |

- La mission 1 de chaque monde est ouverte d'emblée ; chaque mission suivante s'ouvre quand la précédente a au moins une étoile, ce qui ouvre aussi le niveau suivant une fois ses deux missions étoilées.
- Un monde est terminé quand ses six missions ont au moins une étoile. Le meilleur nombre d'étoiles d'une mission ne diminue jamais.

## XP et rang d'aventurier

- Valeur acquise d'une mission = `10 × meilleur nombre de bonnes réponses + 20 si au moins une étoile` (maximum 70, soit 2 520 XP pour les 36 missions).
- À la clôture, seule la différence positive avec ce qui a déjà été versé pour cette mission est attribuée. Exemples vérifiés par les tests : premier 3/5 → +50 ; nouveau 3/5 → 0 ; puis 5/5 → +20 ; premier 2/5 → +20 sans déblocage.
- Rang d'aventurier = `1 + floor(XP / 100)`, purement cosmétique. Les récompenses de rang vont du rang 2 au rang 9 ; au-delà (jusqu'au rang 26 avec les six mondes), le rang continue de monter sans nouvel accessoire.
- Pas d'achat, de dépense d'XP, de classement ni de comparaison entre enfants.

## Récompenses

| Code | Type | Titre | Condition |
|---|---|---|---|
| FIRST_STEP | Badge | Premier pas | Première mission avec au moins une étoile |
| PERFECT_MISSION | Badge | Sans faute | Première mission à trois étoiles |
| NUMBERS_EXPLORER | Badge | Explorateur des Nombres | Monde des Nombres terminé |
| FOREST_GUARDIAN | Badge | Gardien de la Forêt | Forêt du Vivant terminée |
| INVENTOR_GENIUS | Badge | Génie des Inventeurs | Cité des Inventeurs terminée |
| SHAPES_MASTER | Badge | Maître des Formes | Vallée des Formes terminée |
| GREAT_EXPLORER | Badge | Grand Explorateur | Archipel des Explorateurs terminé |
| WORDS_FRIEND | Badge | Ami des Mots | Monde des Mots terminé |
| CAPTAIN_CAP, STAR_CAPE, OWL_FRIEND | Accessoires (tête, dos, compagnon) | Casquette de capitaine, Cape étoilée, Hibou calculateur | Niveaux 1, 2, 3 des Nombres terminés |
| RAINBOW_BAND, EXPLORER_BAG, KITTEN_FRIEND | Accessoires | Bandeau arc-en-ciel, Sac d'explorateur, Chaton messager | Niveaux 1, 2, 3 des Mots terminés |
| MUSHROOM_HAT, NATURALIST_BAG, SQUIRREL_FRIEND | Accessoires | Chapeau champignon, Besace de naturaliste, Écureuil malin | Niveaux 1, 2, 3 de la Forêt terminés |
| INVENTOR_GOGGLES, PROPELLER_PACK, ROBOT_FRIEND | Accessoires | Lunettes d'inventeur, Sac à hélice, Petit robot | Niveaux 1, 2, 3 des Inventeurs terminés |
| TRIANGLE_HAT, PRISM_CAPE, ORIGAMI_CRANE | Accessoires | Chapeau triangle, Cape prisme, Grue en origami | Niveaux 1, 2, 3 des Formes terminés |
| EXPLORER_HAT, SAIL_CAPE, PARROT_FRIEND | Accessoires | Chapeau d'explorateur, Cape voile de navire, Perroquet voyageur | Niveaux 1, 2, 3 des Explorateurs terminés |
| STRAW_HAT … STAR_RABBIT | Accessoires | Chapeau de paille, Tortue sage, Cape de feuilles, Couronne de fleurs, Ailes de papillon, Renard curieux, Casque d'astronaute, Lapin des étoiles | Rangs 2 à 9 atteints |

Chaque récompense est attribuée une seule fois (contrainte unique) ; un accessoire ne peut être équipé que s'il est possédé (vérification serveur).

## Indicateurs du suivi parent

Pour chaque compétence, taux de réussite sur les réponses évaluées des **5 dernières parties terminées** qui la contiennent (les parties abandonnées sont exclues), avec le nombre de questions, de parties et de réussites avec indice :

| Réponses évaluées | Taux | Statut affiché |
|---|---|---|
| moins de 5 | — | En découverte |
| 5 ou plus | < 60 % | À retravailler |
| 5 ou plus | 60 % à < 80 % | En progrès |
| 5 ou plus | ≥ 80 % | Réussites régulières |

Ce sont des indicateurs d'entraînement, pas une attestation de maîtrise du programme. Des pistes de révision sont proposées pour les compétences « À retravailler » et les missions terminées sans étoile.

## Points à valider par un enseignant

- Exactitude, formulation et niveau des 60 questions, des indices et des explications.
- Variantes acceptées pour les réponses en anglais (ex. « thank you » / « thanks »).
- Pertinence des seuils 60 % / 80 % et de la fenêtre de 5 parties.
- Absence de pénalité pour l'indice.
