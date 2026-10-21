# EduQuest CM1 — Cahier des charges fonctionnel et technique

**Version :** 1.0 — 21 octobre 2026  
**Statut :** spécification de développement ; aucun code, test exécuté ou déploiement existant n'est attesté par ce document.  
**Destinataires :** porteur du projet, Claude Code pour la réalisation, Codex pour la revue.  
**Stack imposée :** React + TypeScript, Spring Boot + Java 21, PostgreSQL.

## 1. Origine, portée et conventions

La conversation « Créer une application éducative gamifiée » établit les besoins suivants : application web interactive pour un élève de CM1 ; exercices dans plusieurs matières ; récompenses à chaque niveau ; univers Aventure 3D avec des mondes à explorer ; préparation d'un MVP avec React, Spring Boot et PostgreSQL ; réalisation par Claude Code puis revue par Codex.

**Limite de restitution :** la lecture de la conversation source a restitué les messages utilisateur, mais les anciennes réponses de l'assistant ne contiennent que des références `chatgpt-content-reference`, sans leur texte détaillé ni pièce jointe. Ce fichier rassemble tous les besoins récupérables et les complète par une spécification cohérente. Il ne prétend pas reproduire mot pour mot les réponses inaccessibles. Les noms des mondes, seuils, volumes, schémas et contrats ci-dessous sont des **propositions de cadrage retenues comme valeurs par défaut pour le développement**, modifiables par le porteur du projet.

Les exigences « doit » constituent le contrat MVP. Les fonctionnalités « après MVP » n'en font pas partie. Les validations pédagogique et juridique avant ouverture publique sont des étapes de livraison, pas des affirmations de conformité déjà acquise.

## 2. Vision et objectifs

EduQuest CM1 transforme des séances d'entraînement courtes en aventures. L'enfant choisit un monde, accomplit une mission de cinq exercices, comprend ses erreurs et débloque des étapes et des récompenses cosmétiques. L'adulte suit la progression sans classement entre enfants.

Public : élèves de CM1, généralement autour de 9–10 ans, accompagnés par un parent. Interface française ; contenus d'anglais élémentaires. Usage familial sur ordinateur et tablette, avec adaptation mobile. Une séance cible dure environ 5 à 10 minutes, sans limite de temps imposée pour répondre.

Objectifs mesurables du pilote : un enfant peut démarrer sa première mission après création du profil en moins de trois minutes lors d'un test accompagné ; il peut reprendre une mission interrompue ; l'adulte comprend quelles compétences travailler ; chaque erreur reçoit une explication adaptée. Ces objectifs doivent être évalués, ils ne sont pas des résultats déjà obtenus.

### 2.1 Les six mondes de la vision complète

| Monde proposé | Matière | Compétences et exemples | Présence MVP |
|---|---|---|---|
| Les Îles des Nombres | Mathématiques | Numération, opérations, problèmes simples, fractions usuelles | Jouable, périmètre réduit ci-dessous |
| Le Royaume des Mots | Anglais | Salutations, nombres, couleurs, animaux, vocabulaire courant | Jouable |
| La Vallée des Formes | Géométrie | Figures, angles droits, symétrie, repérage | Aperçu verrouillé |
| L'Archipel des Explorateurs | Géographie | Cartes, paysages, territoires, repérage | Aperçu verrouillé |
| La Forêt du Vivant | Sciences | Vivant, environnement, besoins des êtres vivants | Aperçu verrouillé |
| La Cité des Inventeurs | Physique et technologie | Matière, mouvement, énergie, objets et expériences simples | Aperçu verrouillé |

« Physique » est un thème de découverte scientifique adapté au primaire, pas un programme de physique du secondaire. Les frontières Sciences/Physique et Maths/Géométrie servent la navigation ; les compétences peuvent se recouper. La correspondance exacte au programme CM1 devra être validée par un enseignant avant publication.

### 2.2 Direction artistique et 3D réaliste pour le MVP

Univers coloré, chaleureux, explorateur, îles flottantes et chemins visibles. Avatar simple issu d'un catalogue local, compagnon-guide, animations brèves. Pas de monde ouvert, combat, violence ou déplacement libre complexe.

Le MVP doit proposer **une carte 3D légère avec caméra fixe** (Three.js via React Three Fiber ou équivalent documenté), utilisant des formes simples et des assets locaux. Les interactions sont doublées par une liste HTML des mondes et missions. Le questionnaire reste en HTML accessible. Si WebGL est indisponible, lent ou désactivé, afficher automatiquement une carte 2D équivalente avec toutes les actions. Une préférence « animations réduites » arrête les mouvements décoratifs. Ne pas bloquer l'apprentissage sur le chargement de la scène 3D.

## 3. Périmètre de livraison

### 3.1 MVP obligatoire

- Inscription et connexion d'un parent, vérification d'adresse en production, réinitialisation de mot de passe.
- Jusqu'à cinq profils enfants par parent : pseudonyme, avatar, classe CM1 ; aucune adresse e-mail enfant.
- Sélection de profil après connexion adulte ; mode enfant distinct et sortie protégée par réauthentification adulte.
- Carte des six mondes, seuls Maths et Anglais étant jouables.
- Trois niveaux séquentiels par monde ; deux missions de cinq questions par niveau : **12 missions et 60 exercices publiés distincts**.
- Types d'exercice : QCM à réponse unique, réponse numérique, texte court pour l'anglais. Pas de correction par IA.
- Consignes, indice, correction immédiate, explication, bilan de mission, reprise de partie.
- Étoiles, XP, niveaux d'aventurier, badges et cosmétiques déterministes.
- Tableau de bord enfant et suivi parent ; préférences de son et d'animation.
- Export et suppression des données du compte familial ; contenus administrés par fichiers versionnés et import validé.
- Environnement local reproductible, tests, documentation et préparation au déploiement.

### 3.2 Après MVP

Quatre mondes supplémentaires jouables ; enseignant et classes ; affectation d'exercices ; véritable éditeur de contenu ; audio natif enregistré et exercices d'écoute ; déplacement 3D libre ; adaptation pédagogique avancée ; application mobile native ; multilinguisme de l'interface ; usage hors ligne synchronisé.

### 3.3 Exclusions

Publicité, achats intégrés, monnaie réelle, chat, messages publics, classement social, profils publics, liens externes dans l'espace enfant, génération de questions par IA, suivi publicitaire, microphone, caméra et géolocalisation. Aucune pénalité de série quotidienne ni récompense aléatoire de type coffre payant.

## 4. Rôles et autorisations

| Rôle/contexte | Droits |
|---|---|
| Visiteur | Présentation, confidentialité, inscription/connexion parent ; aucun accès aux données élèves |
| Parent authentifié | Gérer ses seuls profils enfants, consulter leurs résultats, exporter/supprimer ses données, accéder aux paramètres adultes |
| Session enfant | Lire le catalogue publié et ses propres progrès, jouer, choisir ses cosmétiques ; aucun accès aux données du parent ni aux autres enfants |
| Administrateur technique | Importer les contenus par outil hors interface, vérifier l'exploitation ; aucun droit métier implicite de lecture globale des enfants |
| Enseignant | Rôle futur ; aucune route ni permission MVP |

Toutes les vérifications de propriété sont réalisées côté serveur. Un UUID difficile à deviner ne remplace jamais une autorisation. L'administrateur n'est pas un compte exposé par inscription publique.

## 5. Écrans et états attendus

| Écran / route indicative | Contenu et actions |
|---|---|
| Accueil `/` | Proposition, univers, accès parent, lien confidentialité |
| Compte `/connexion`, `/inscription` | Formulaires adultes, messages neutres, aide et récupération |
| Vérification/récupération | Jetons à usage unique, expiration et états succès/échec |
| Famille `/parent/enfants` | Liste, création, modification, suppression avec confirmation |
| Choix du profil `/profils` | Avatar + pseudonyme ; sélection uniquement depuis session adulte |
| Carte `/aventure` | Six mondes, progression, mondes futurs indiqués « Bientôt disponible », équivalent HTML |
| Monde `/mondes/:slug` | Trois niveaux, missions, étoiles, conditions de déblocage, bouton reprendre |
| Mission `/missions/:id/jouer` | Une question à la fois, progression 1/5, consigne, saisie, indice, valider, feedback, continuer, quitter |
| Bilan `/parties/:id/bilan` | Réponses correctes au premier essai, étoiles, XP nouvellement gagnés, compétences, rejouer/suivant |
| Profil `/mon-aventurier` | Niveau, badges, cosmétiques possédés, équipement |
| Suivi `/parent/suivi/:childId` | Compétences, résultats récents, tentatives, propositions de révision |
| Paramètres `/parametres` | Son, réduction des animations, accès adulte protégé |
| Données `/parent/confidentialite` | Informations, export, suppression, versions d'information/accord |
| Erreurs | Page introuvable, session expirée, accès interdit, panne réseau, maintenance |

Chaque écran de données prévoit chargement, vide, erreur et succès. Chaque bouton envoyé au serveur est protégé contre le double clic. Le retour navigateur ne doit pas annuler une réponse enregistrée. Un rechargement reconstruit l'état à partir du serveur.

### 5.1 Accessibilité et ergonomie

Navigation clavier intégrale, focus visible et restauré après changement d'écran, libellés explicites, annonces des corrections aux lecteurs d'écran. Pas d'information exclusivement par couleur, son ou animation. Cibles tactiles d'au moins 44 × 44 pixels ; texte agrandissable à 200 %, contrastes visant WCAG 2.2 AA. Contenu principal utilisable dès 360 px ; pas de glisser-déposer obligatoire. Respect de `prefers-reduced-motion`, son désactivé par défaut. Pas de minuteur anxiogène. Explication d'erreur courte et encourageante, sans jugement sur l'enfant.

## 6. Parcours utilisateurs

### P1 — Première utilisation

1. L'adulte crée et vérifie son compte, lit les informations nécessaires.
2. Il crée un profil avec un pseudonyme et choisit un avatar.
3. Il entre en mode enfant ; le serveur remplace la session adulte par une session limitée.
4. L'enfant découvre la carte puis choisit Maths ou Anglais ; le niveau 1 est ouvert dans les deux mondes.
5. Il lance la première mission, répond aux cinq questions, lit les corrections et obtient son bilan.
6. La progression persiste ; la carte montre les étapes accessibles.

### P2 — Reprise et déconnexion

Une mission interrompue conserve les réponses déjà confirmées. Au retour, la prochaine question non terminée apparaît. Sans session, l'adulte se reconnecte puis sélectionne l'enfant. « Quitter » propose enregistrer et revenir à la carte. La déconnexion révoque la session serveur. Un autre onglet reçoit une erreur de session ou recharge l'état ; il ne peut pas modifier une question déjà terminée.

### P3 — Difficulté et révision

Après une erreur, afficher l'explication sans retirer d'XP acquis. L'enfant termine la mission puis peut la rejouer. Le parent voit une compétence « À retravailler » avec les effectifs observés, sans diagnostic scolaire automatique.

### P4 — Espace parent

Depuis le mode enfant, l'accès adulte nécessite e-mail et mot de passe du parent propriétaire. Le serveur ne restaure pas des privilèges à partir d'un simple drapeau local. L'adulte consulte le suivi, change de profil ou exporte/supprime ses données.

## 7. Règles pédagogiques et contenus

### 7.1 Structure

Un monde contient trois niveaux ordonnés ; un niveau contient deux missions ordonnées. Une mission contient cinq exercices, chacun rattaché à une compétence principale. La difficulté pédagogique vaut 1, 2 ou 3 et reste distincte du niveau d'aventurier.

Progression Maths proposée : (1) nombres entiers et additions/soustractions ; (2) multiplication et partage exact ; (3) problèmes courts à une ou deux étapes adaptées. Anglais : (1) salutations et couleurs ; (2) nombres et animaux ; (3) objets de la classe et phrases très simples. Les 30 questions par matière se répartissent à raison de 10 par niveau. Les fractions et l'écoute audio restent hors corpus initial.

Les 60 exercices doivent être écrits et relus ; ils ne peuvent pas être des espaces réservés ou des répétitions artificielles. Chaque question comporte consigne, contenu, bonne réponse privée, indice, explication et difficulté. Les exercices ne doivent pas demander de raconter sa vie personnelle ou de saisir une donnée sensible.

### 7.2 Validation et correction

- QCM : au moins trois options, identifiants stables, exactement une réponse correcte ; comparer les identifiants, jamais les positions.
- Numérique : format décimal explicite, virgule ou point accepté, espaces périphériques ignorés ; comparaison exacte après normalisation décimale, sans flottants binaires. Unité affichée séparément et imposée dans la consigne.
- Texte anglais : trim, casse insensible, espaces internes regroupés ; liste fermée de variantes autorisées définie par le rédacteur. Ne pas accepter automatiquement toute faute d'orthographe ; apostrophes typographiques normalisées si nécessaire.
- Une seule réponse évaluée par question et par partie. Après validation, correction affichée et passage à la suivante. Une nouvelle tentative nécessite de rejouer la mission.
- Indice facultatif avant validation ; pas de pénalité au MVP. Le suivi enregistre son utilisation et la réussite associée, sans l'assimiler à une réussite autonome.
- Une réponse vide ou de format invalide reste non évaluée et ne consomme pas la question.
- La bonne réponse et l'explication sont renvoyées seulement après la soumission correspondante. Les corrigés ne figurent pas dans le bundle client ni dans l'API catalogue enfant.

### 7.3 Exemples de référence

| Type | Consigne | Réponse privée | Explication |
|---|---|---|---|
| Nombre | Calcule 245 + 132. | 377 | Additionne les unités, puis les dizaines, puis les centaines. |
| QCM | Comment dit-on « bleu » en anglais ? `blue`, `red`, `green` | Identifiant de `blue` | « Blue » signifie « bleu ». |
| Texte | Écris « chat » en anglais. | `cat` | « Cat » signifie « chat ». |

### 7.4 Indicateurs pédagogiques

Pour chaque compétence, calculer le taux de réussite sur les réponses évaluées des cinq dernières parties terminées contenant cette compétence. Afficher aussi le nombre de questions, le nombre de parties et le recours aux indices. Avant cinq réponses évaluées : « En découverte ». Ensuite : moins de 60 % « À retravailler », de 60 à moins de 80 % « En progrès », au moins 80 % « Réussites régulières ». Ce sont des indicateurs d'entraînement, pas une attestation de maîtrise du programme. Une partie abandonnée ne contribue pas à cet indicateur.

## 8. Gamification déterministe

### 8.1 Étoiles et déblocages

Sur cinq questions évaluées : 0–2 bonnes réponses = 0 étoile ; 3 = 1 ; 4 = 2 ; 5 = 3. Le score d'une partie est le nombre de réponses correctes, y compris avec indice, affiché avec cet indicateur.

La mission 1 de chaque monde est accessible immédiatement. La mission suivante se débloque lorsque la précédente atteint au moins une étoile. Le niveau suivant se débloque lorsque ses deux missions précédentes ont chacune au moins une étoile. Un monde est terminé lorsque ses six missions ont au moins une étoile. Le meilleur nombre d'étoiles par mission ne diminue jamais.

### 8.2 XP sans exploitation par répétition

La valeur maximale acquise pour une mission est `10 × meilleur_nombre_de_bonnes_réponses + 20 si la mission a déjà obtenu au moins une étoile`. À la clôture, attribuer seulement la différence positive entre cette valeur et le total déjà attribué pour cette mission. Une première réussite 3/5 rapporte 50 XP ; un nouveau 3/5 rapporte 0 ; une amélioration à 5/5 rapporte 20 de plus. Une première partie 2/5 rapporte 20 XP mais ne débloque pas la suite. Maximum : 70 XP par mission, 840 XP pour les douze missions.

Le niveau d'aventurier vaut `1 + floor(XP_total / 100)`. Il s'agit d'un rang cosmétique, sans effet sur les compétences ou le déblocage des missions. Une récompense de catalogue est attribuée une seule fois à chaque rang atteint ; prévoir les rangs 2 à 9 pour le MVP. Aucun mécanisme d'achat ni dépense d'XP.

### 8.3 Récompenses

Badge « Premier pas » à la première mission terminée avec au moins une étoile ; « Explorateur des Nombres » et « Ami des Mots » au monde terminé ; « Sans faute » à la première mission à trois étoiles. Un accessoire d'avatar est attribué à chaque niveau pédagogique terminé. Catalogue local d'avatars et accessoires, chaque ressource accompagnée de sa licence.

Attributions, progression, solde XP et clôture de partie doivent être atomiques. Une contrainte unique empêche tout doublon de récompense. Deux requêtes concurrentes ne peuvent ni doubler l'XP ni terminer deux fois une partie.

## 9. Architecture cible

Monorepo, application React TypeScript construite avec Vite, routage, client HTTP typé, gestion du cache serveur (par exemple TanStack Query), composants accessibles et tests Vitest/Testing Library. Scène 3D chargée à la demande. Les préférences non sensibles peuvent être conservées localement ; les réponses et droits sont gérés par le serveur.

Backend : Java 21, Spring Boot dans une version stable compatible choisie et figée à l'initialisation, Spring Web, Validation, Security, Data JPA, Flyway, PostgreSQL. Monolithe modulaire : `auth`, `family`, `catalog`, `learning`, `rewards`, `privacy`. Controllers → services transactionnels → repositories. DTO séparés des entités JPA, pas de sérialisation directe des entités.

API `/api/v1`, documentation OpenAPI versionnée. Session opaque serveur via cookie HttpOnly ; pas de JWT en localStorage. PostgreSQL persiste les sessions via Spring Session JDBC. En production, servir frontend et API sous la même origine via reverse proxy HTTPS. En développement, proxy Vite `/api` vers le backend ; pas de CORS permissif.

Le serveur est la seule autorité pour l'ordre des questions, les corrections, l'ouverture des niveaux et les récompenses. Les missions utilisent des versions publiées immuables ; une modification de contenu crée une nouvelle version, sans altérer une partie en cours ou un historique.

## 10. Modèle de données détaillé

Conventions : PK UUID sauf tables techniques du framework ; horodatages `timestamptz` UTC ; textes bornés ; clés étrangères réelles ; montants XP entiers positifs ou nuls ; codes métier uniques. Les ordres sont des entiers strictement positifs. Les données JSONB suivent des schémas validés à l'import et côté service.

| Table | Champs principaux et contraintes |
|---|---|
| `parent_account` | `id`, `email_normalized` unique, `password_hash`, `email_verified_at` nullable, `status` ACTIVE/PENDING/DELETING, `created_at`, `last_login_at` |
| `account_token` | `id`, `parent_id` FK cascade, `type` VERIFY_EMAIL/RESET_PASSWORD, `token_hash` unique, `expires_at`, `used_at`, `created_at` ; jamais de jeton brut stocké |
| `child_profile` | `id`, `parent_id` FK cascade, `nickname` 2–30 caractères, `avatar_code`, `grade` CM1, `created_at`, `updated_at` ; maximum cinq contrôlé transactionnellement |
| `privacy_acknowledgement` | `id`, `parent_id` FK cascade, `child_id` nullable FK cascade, `notice_version`, `purpose_code`, `legal_basis`, `parent_action`, `child_information_version`, `recorded_at`, `withdrawn_at` nullable ; pas de consentement fictif global |
| `world` | `id`, `slug` unique, `title`, `subject_code` unique, `display_order` unique, `availability` PLAYABLE/COMING_SOON, `asset_key` |
| `level` | `id`, `world_id` FK, `ordinal`, `title` ; unique `(world_id, ordinal)` |
| `mission` | `id`, `level_id` FK, `ordinal`, `code` unique ; unique `(level_id, ordinal)` ; identité stable |
| `mission_version` | `id`, `mission_id` FK, `version_number`, `title`, `status` DRAFT/PUBLISHED/RETIRED, `published_at` ; unique `(mission_id, version_number)` ; une version courante publiée par mission via index partiel |
| `skill` | `id`, `code` unique, `subject_code`, `label`, `description` |
| `exercise` | `id`, `mission_version_id` FK, `ordinal`, `skill_id` FK, `type` MCQ/NUMBER/TEXT, `difficulty` 1..3, `prompt`, `public_payload` JSONB, `answer_key` JSONB privé, `hint`, `explanation` ; unique `(mission_version_id, ordinal)` |
| `attempt` | `id`, `child_id` FK cascade, `mission_id` FK, `mission_version_id` FK, `status` IN_PROGRESS/COMPLETED/ABANDONED, `started_at`, `completed_at`, `correct_count` 0..5 nullable, `stars` 0..3 nullable, `version` pour verrou optimiste |
| `attempt_answer` | `id`, `attempt_id` FK cascade, `exercise_id` FK, `submitted_answer` JSONB, `is_correct`, `hint_used`, `hint_requested_at`, `answered_at` nullable ; unique `(attempt_id, exercise_id)` ; une ligne peut exister pour un indice avant la réponse |
| `mission_progress` | `id`, `child_id` FK cascade, `mission_id` FK, `best_correct_count` 0..5, `best_stars` 0..3, `xp_awarded` 0..70, `completed_attempts`, `last_completed_at` ; unique `(child_id, mission_id)` |
| `reward_definition` | `id`, `code` unique, `type` BADGE/COSMETIC, `title`, `asset_key`, `rule_code` unique, `rule_parameters` JSONB ; règles interprétées par code autorisé, jamais par script |
| `child_reward` | `id`, `child_id` FK cascade, `reward_id` FK, `granted_at`, `source_attempt_id` nullable FK ; unique `(child_id, reward_id)` |
| `child_equipment` | `child_id` FK cascade, `slot`, `reward_id` FK ; PK `(child_id, slot)` ; vérifier la possession dans le service |
| `xp_event` | `id`, `child_id` FK cascade, `attempt_id` FK cascade unique, `mission_id` FK, `amount` >0, `created_at` ; journal des gains, pas d'événement pour un delta nul |
| `audit_event` | `id`, `event_type`, `actor_pseudonymous_ref` nullable, `outcome`, `created_at`, `correlation_id` ; aucun mot de passe, réponse, e-mail ou jeton |

Prévoir aussi les tables Spring Session et leurs migrations compatibles. Le niveau et le total XP sont dérivés de `mission_progress`/`xp_event`, pas maintenus indépendamment sans invariant vérifié.

Index : enfants par parent ; parties par enfant/date ; réponses par partie ; progression par enfant ; jetons par expiration ; sessions par expiration. Index partiel unique sur `(child_id, mission_id)` pour les parties `IN_PROGRESS`. Verrouiller la ligne enfant pendant la création de partie et pendant les attributions. Ordre global pour toutes les mutations : enfant, puis partie si nécessaire, puis progression ; ne jamais prendre ces verrous dans l’ordre inverse. Une clôture vérifie à nouveau l’état après acquisition des verrous.

Relations : parent 1→N enfants ; monde 1→N niveaux 1→N missions 1→N versions 1→5 exercices ; enfant 1→N parties 1→N réponses ; enfant N↔N récompenses. Pas de suppression de contenu référencé par l'historique. La suppression familiale cascade sur les données personnelles et retire les références d'acteur des journaux conservés.

## 11. API REST et contrats

### 11.1 Conventions

JSON UTF-8 ; dates ISO 8601 UTC ; UUID dans les chemins ; pagination `page` base 0, `size` défaut 20, maximum 100 pour les listes volumineuses. Erreurs `application/problem+json` : `type`, `title`, `status`, `code`, `detail` non sensible, `instance`, `traceId`, `fieldErrors` éventuel. Pas de stack trace publique.

Codes : 400 format invalide, 401 session absente/expirée, 403 rôle insuffisant ou CSRF invalide, 404 ressource inexistante ou appartenant à une autre famille, 409 état concurrent/incompatible, 422 réponse syntaxiquement valide mais incompatible avec le type attendu, 429 limitation, 500 erreur interne neutre. Les validations de formulaire usuelles utilisent 400.

### 11.2 Authentification et famille — contexte parent sauf indication

| Méthode et route `/api/v1` | Entrée / sortie et règle |
|---|---|
| GET `/auth/csrf` | Public ; initialise le jeton CSRF et renvoie son nom d'en-tête et sa valeur |
| POST `/auth/register` | Public + CSRF : `{email,password,noticeVersion}` ; 202 neutre, création PENDING et envoi vérification si admissible |
| POST `/auth/verify-email` | Public + CSRF : `{token}` ; 204 ; usage unique, expiration 24 h |
| POST `/auth/login` | Public + CSRF : `{email,password}` ; 200 `{role:"PARENT"}` et session renouvelée ; compte vérifié requis en production |
| POST `/auth/forgot-password` | Public + CSRF : `{email}` ; 202 toujours identique |
| POST `/auth/reset-password` | Public + CSRF : `{token,newPassword}` ; 204 ; expiration 30 min, révocation des sessions |
| GET `/auth/me` | Parent ou enfant ; retourne seulement identité/contexte autorisés, jamais données parent en mode enfant |
| POST `/auth/logout` | Parent ou enfant + CSRF ; 204, invalide la session |
| GET/POST `/children` | Liste des siens / `{nickname,avatarCode,grade:"CM1"}` → 201 profil et Location |
| PATCH `/children/{id}` | Champs éditables autorisés uniquement → 200 |
| DELETE `/children/{id}` | Réauthentification parent récente et confirmation → 204 suppression de toutes ses données |
| POST `/auth/child-session` | Parent : `{childId}` → 200 contexte CHILD ; invalide l'ancienne session, crée une session limitée |
| POST `/auth/parent-session` | Enfant : `{email,password}` du parent propriétaire → 200 contexte PARENT et nouvelle session ; rate limit renforcé |
| GET `/children/{id}/progress` | Parent propriétaire : agrégats, compétences et historique paginé |
| GET `/privacy/export` | Parent réauthentifié récemment ; téléchargement JSON versionné de toutes ses données familiales ; `Cache-Control: no-store` |
| DELETE `/account` | Parent réauthentifié depuis moins de cinq minutes, `{confirmation:"SUPPRIMER"}` → 204 ; révocation et suppression atomique |

### 11.3 Apprentissage — contexte enfant uniquement

| Méthode et route `/api/v1` | Contrat |
|---|---|
| GET `/worlds` | Six mondes, disponibilité et progression propre |
| GET `/worlds/{slug}` | Niveaux/missions et état LOCKED/AVAILABLE/COMPLETED calculé |
| GET `/me/progress` | XP, rang, étoiles, badges et progression par monde |
| POST `/missions/{id}/attempts` | `{}` → 201 nouvelle partie ou 200 partie en cours existante ; vérifie déblocage/version publiée ; ne reçoit aucun score |
| GET `/attempts/{id}` | État, nombre de réponses, question courante publique ou bilan si clôturée |
| POST `/attempts/{id}/answers/{exerciseId}/hint` | `{}` → indice ; enregistrement idempotent avant réponse |
| PUT `/attempts/{id}/answers/{exerciseId}` | `{answer:...}` → correction et progression ; même réponse répétée retourne le même résultat, réponse différente après validation → 409 |
| POST `/attempts/{id}/complete` | `{}` → bilan ; seulement après cinq réponses ; répétition retourne le bilan mémorisé, dont `xpDelta` de clôture |
| POST `/attempts/{id}/abandon` | `{}` → 204 ; répétable ; aucun gain ; impossible après clôture |
| GET `/me/rewards` | Catalogue possédé + équipement |
| PUT `/me/equipment/{slot}` | `{rewardCode}` → équipement après vérification de possession |

Seule la question courante peut recevoir une réponse. Chaque question doit appartenir à la version de cette partie. Les routes d'apprentissage utilisent l'enfant de la session, jamais un `childId` arbitraire dans le corps. Les données historiques peuvent être consultées uniquement via les routes prévues pour leur propriétaire.

### 11.4 Exemples de payload

Question publique :

```json
{"attemptId":"uuid","status":"IN_PROGRESS","answeredCount":0,"currentQuestion":{"id":"uuid","position":1,"total":5,"type":"MCQ","prompt":"Comment dit-on bleu ?","options":[{"id":"a","label":"blue"},{"id":"b","label":"red"},{"id":"c","label":"green"}]}}
```

Soumission : `{"answer":{"optionId":"a"}}`, `{"answer":{"number":"377"}}` ou `{"answer":{"text":"cat"}}` selon le type. Rejeter les clés supplémentaires sensibles (`score`, `xp`, `isCorrect`).

Réponse de correction :

```json
{"exerciseId":"uuid","correct":true,"hintUsed":false,"expectedAnswer":{"optionId":"a"},"explanation":"Blue signifie bleu.","answeredCount":1,"readyToComplete":false}
```

Bilan : `{"attemptId":"uuid","correctCount":3,"questionCount":5,"stars":1,"xpDelta":50,"xpTotal":50,"adventurerLevel":1,"newRewards":["FIRST_STEP"],"unlockedMissionIds":["uuid"]}`. Une répétition renvoie le même bilan d'événement ; pour le total actuel, utiliser `/me/progress`. L'interface n'affiche la célébration qu'une fois par identifiant de partie.

## 12. Sécurité, données personnelles et mineurs

### 12.1 Mesures techniques exigées

- Hachage des mots de passe par Argon2id via Spring Security, paramètres de mémoire et de coût documentés et calibrés ; minimum 12 caractères, maximum 128, espaces autorisés ; pas de mot de passe tronqué silencieusement.
- Cookies de session HttpOnly, Secure en production, SameSite=Lax ; identifiant renouvelé à chaque changement de privilège. Cookie local non Secure autorisé uniquement dans le profil de développement HTTP.
- Protection CSRF sur toutes les mutations, y compris connexion et déconnexion ; requêtes JSON avec en-tête CSRF, jamais de mutation par GET. Renouveler le jeton après changement de session.
- Session parent : expiration après 30 minutes d'inactivité, durée absolue 12 h ; enfant : 60 minutes d'inactivité, durée absolue 12 h. La limite absolue doit être explicitement implémentée.
- Rate limit persistant ou partagé : tentatives d'authentification limitées par compte et origine, récupération et envois e-mail bornés ; 429 et `Retry-After`. Ne pas bloquer définitivement un compte à distance.
- DTO validés, taille de requête bornée, requêtes paramétrées, pas de HTML arbitraire dans les contenus, pas d'upload utilisateur MVP. Protection XSS, CSP compatible avec les ressources locales, `nosniff`, politique referrer restrictive, HTTPS et HSTS en production.
- Secrets par environnement ou gestionnaire de secrets ; `.env` ignoré ; aucune clé réelle dans le dépôt. PostgreSQL non exposé publiquement ; utilisateur applicatif à droits minimaux ; sauvegardes chiffrées et restauration testée.
- Journaux structurés sans réponses enfant, cookies, jetons, mots de passe ni corps des requêtes. Identifiants pseudonymes restent des données personnelles et sont protégés.
- Toute route portant un identifiant fait l'objet de tests d'accès inter-familles. Pas de cache partagé des réponses personnalisées.

### 12.2 Cadrage RGPD avant pilote public

Le responsable de traitement et ses coordonnées doivent être identifiés, les finalités et bases légales documentées séparément : compte adulte, suivi pédagogique, sécurité, communications strictement transactionnelles. Le consentement n'est pas une base universelle et un simple bouton « j'accepte le RGPD » ne suffit pas.

La CNIL indique qu'en France le seuil de consentement autonome pour les traitements concernés des services en ligne est de 15 ans. Pour un enfant plus jeune, lorsqu'un traitement repose sur ce consentement, prévoir l'accord conjoint de l'enfant et du titulaire de l'autorité parentale avec information adaptée. Le mode familial et la vérification e-mail constituent des mesures produit ; ils ne prouvent pas à eux seuls l'autorité parentale. Le mécanisme proportionné de vérification et les bases légales doivent être validés avant ouverture publique.

Sources officielles consultées le 21 octobre 2026 : [CNIL — Consentement](https://www.cnil.fr/les-bases-legales/consentement), [recommandation sur l'accord parental](https://www.cnil.fr/fr/recommandation-4-rechercher-le-consentement-dun-parent-pour-les-mineurs-de-moins-de-15-ans), [vérification respectueuse de la vie privée](https://cnil.fr/fr/recommandation-7-verifier-lage-de-lenfant-et-laccord-des-parents-dans-le-respect-de-sa-vie-privee), [principes et minimisation](https://www.cnil.fr/fr/reglement-europeen-protection-donnees/chapitre2).

Exigences produit : pas de nom complet enfant, date de naissance précise, école, adresse, photo réelle ou donnée de santé. Pseudonyme conseillé non identifiant. Fournir une notice adulte et une version enfant très simple. Aucun traceur non nécessaire au MVP ; documenter les cookies strictement nécessaires sans fabriquer un consentement obligatoire pour ceux-ci.

Politique proposée à faire valider : réponses et progression conservées pendant l'activité du compte ; après 12 mois d'inactivité familiale, notification adulte puis suppression 30 jours plus tard ; journaux de sécurité 90 jours ; sauvegardes à rotation maximale 30 jours. Programmer ces purges et les tester avec une horloge contrôlable. La suppression retire immédiatement les données de la base active ; les sauvegardes expirent selon leur rotation et tout processus de restauration doit réappliquer les suppressions enregistrées dans un registre minimal protégé. Justifier la conservation de ce registre et le purger à son tour.

Prévoir accès, rectification, export, suppression et contact pour les autres droits ; vérifier l'identité de manière proportionnée. Décrire hébergement, sous-traitants, localisation et éventuels transferts. Privilégier une région UE et contractualiser les prestataires retenus. Évaluer et documenter la nécessité d'une AIPD selon le contexte réel et les risques pour les enfants ; ne pas affirmer qu'elle est automatiquement faite ou inutile.

Le contexte scolaire, s'il est ajouté, demande un cadrage distinct des responsabilités et bases légales. Pas d'activation d'un rôle enseignant caché dans le MVP.

## 13. Structure du dépôt attendue

```text
eduquest-cm1/
  README.md
  EDUQUEST_CM1_SPECIFICATIONS.md
  .env.example
  .gitignore
  compose.yaml
  Makefile
  frontend/
    package.json
    package-lock.json
    src/
      app/
      features/{auth,family,worlds,missions,progress,rewards,privacy}/
      components/
      api/
      scene/
      styles/
    public/assets/
    tests/
  backend/
    pom.xml
    mvnw
    .mvn/
    src/main/java/.../{auth,family,catalog,learning,rewards,privacy,shared}/
    src/main/resources/{db/migration,application.yml,application-dev.yml}/
    src/test/java/
  content/
    schema/
    worlds/
    maths/
    english/
    rewards/
  e2e/
  infra/
  docs/
    architecture.md
    api/openapi.yaml
    pedagogy.md
    security-privacy.md
    content-authoring.md
    assets-licenses.md
    deployment.md
    review-checklist.md
  .github/workflows/ci.yml
```

Le nom du package Java et les versions exactes des dépendances sont choisis au démarrage puis verrouillés. Prévoir un wrapper Maven et un lockfile npm. Les comptes de démonstration et services de capture d'e-mails ne sont chargés qu'avec un profil local explicitement activé.

## 14. Contenus, migrations et données de démonstration

Flyway crée le schéma et les contraintes. `ddl-auto=validate`, jamais `update` en production. L'import de contenu est un outil CLI Spring dédié, idempotent par code/version et transactionnel ; il valide schémas JSON, nombre de questions, types, variantes, clés de réponse, références aux assets et compétences. Aucun corrigé ne doit être copié dans `frontend/public`.

Une version publiée est immuable. Pour corriger un contenu, retirer l'ancienne version des nouvelles parties et publier la suivante ; les parties commencées restent attachées à l'ancienne. Les déblocages et XP se rattachent à l'identité stable de mission, empêchant de regagner 70 XP lors d'une simple republication. Les différences pédagogiques majeures nécessitent une nouvelle mission, hors MVP initial.

Seed de démonstration : six mondes, douze missions publiées, soixante exercices, compétences, catalogue de badges/accessoires, un parent et deux enfants fictifs uniquement en développement. Pas de données réelles. L'import deux fois ne doit créer aucun doublon. Aucun mot de passe de démonstration actif en production.

## 15. Backlog ordonné et critères d'acceptation

| Lot | Priorité | Travail et résultat vérifiable |
|---|---|---|
| B01 | P0 | Initialiser monorepo, versions, wrappers, Compose, CI ; base vide migrée et applications démarrables |
| B02 | P0 | Schéma et import contenu ; 6 mondes/12 missions/60 questions valides, réimport sans doublon |
| B03 | P0 | Compte parent, e-mails, sessions, CSRF et permissions ; accès non autorisés refusés |
| B04 | P0 | Profils et changement de contexte ; aucune élévation depuis le mode enfant |
| B05 | P0 | Moteur pédagogique, correction, reprise ; réponses déterministes, privées avant soumission |
| B06 | P0 | Clôture, XP, étoiles, déblocages ; règles §8 et concurrence vérifiées |
| B07 | P0 | Parcours React complet et cartes HTML ; enfant peut terminer les deux mondes |
| B08 | P0 | Carte 3D et repli 2D ; toutes les actions accessibles sans WebGL |
| B09 | P0 | Tableau parent, indicateurs et récompenses équipables ; données du bon enfant seulement |
| B10 | P0 | Export, suppression, purges, notices et sécurité d'exploitation |
| B11 | P0 | Tests, audit accessibilité, documentation, livraison reproductible et rapport de limites |
| B12 | P1 après MVP | Éditeur, classes, audio et autres mondes, à spécifier séparément |

### Scénarios de recette obligatoires

1. **Compte** : adresse vérifiée, connexion puis création d'un enfant ; un compte non vérifié n'accède pas aux données en profil production.
2. **Isolation** : un parent B et un enfant B reçoivent 404 pour les ressources de A ; l'enfant A reçoit 403 sur les routes parent même si le frontend est contourné.
3. **Première mission** : avec trois bonnes réponses sur cinq, une étoile et 50 XP sont attribués ; la mission suivante s'ouvre.
4. **Amélioration** : rejouer 3/5 ne rapporte rien ; passer ensuite à 5/5 rapporte 20 XP et donne trois étoiles, sans doublon de badge.
5. **Concurrence** : deux clôtures simultanées renvoient un bilan cohérent, un seul gain et une seule attribution par récompense.
6. **Reprise** : après deux réponses puis fermeture/reconnexion, la troisième question apparaît ; les réponses passées sont conservées et non modifiables.
7. **Contournement** : impossible de répondre à un exercice d'une autre partie, sauter une question, envoyer son propre score ou jouer un niveau verrouillé.
8. **Mondes futurs** : géométrie, géographie, sciences et physique affichent un aperçu sans mission lançable ; l'API refuse leur lancement.
9. **Fallback** : WebGL désactivé, carte navigable au clavier et mission terminable ; réduction des animations respectée.
10. **Données** : export familial complet sans hash ni secret ; suppression retire profils, réponses, progression, sessions et jetons ; les données de l'autre famille subsistent.
11. **Contenus** : une nouvelle version n'altère pas une partie en cours ni le plafond XP de la mission.
12. **Erreurs réseau** : un envoi dont la réponse réseau est perdue peut être répété sans nouvelle réponse ni gain supplémentaire.

## 16. Stratégie de tests et qualité

Backend JUnit 5 : normalisation numérique et texte, grille d'étoiles aux seuils 2/3/4/5, delta XP, badges, progression, indices, versions de contenus et invariants. Tests d'intégration avec Testcontainers **PostgreSQL réel**, Flyway, sessions, CSRF, propriété des données, unicité, suppression et concurrence. Ne pas remplacer PostgreSQL par H2 pour les tests de contraintes et JSONB.

Frontend Vitest + Testing Library : formulaires, correction, reprise, états chargement/erreur, absence de double soumission, gestion session expirée, équipement. Tests du contrat des DTO publics : absence de `answerKey` et explication avant soumission.

E2E Playwright : inscription via boîte mail locale, création de profil, choix du monde, mission, bilan, reprise, passage parent, export/suppression ; au moins un parcours mobile et un parcours sans WebGL. Tests d'accessibilité automatisés complétés par clavier, zoom 200 %, lecteur d'écran sur carte/mission/bilan et réduction des animations.

CI : compilation Java 21, tests unitaires/intégration, typecheck et build TypeScript, lint, E2E essentiels, validation contenu, contrôle de secrets et dépendances. Les résultats doivent être rapportés avec commandes réellement exécutées ; un test non exécuté reste explicitement signalé.

Objectifs performance à mesurer sur un environnement documenté : interactions et soumission sans attente artificielle ; API p95 inférieur à 500 ms hors e-mail pour 20 enfants actifs simultanés ; écran principal utilisable en moins de 3 secondes sur connexion de test définie, carte 3D asynchrone ; minimum 30 images/s sur matériel cible sinon repli 2D. Fournir protocole, matériel et résultats, pas une garantie non mesurée.

## 17. Configuration et commandes de démarrage à fournir

**Contrat à implémenter : les commandes suivantes sont attendues dans le futur dépôt ; elles ne sont pas attestées comme fonctionnelles aujourd'hui.** Pré-requis : Java 21, Docker/Compose, version Node LTS compatible à figer dans `.nvmrc`, npm avec lockfile. Aucun outil cloud payant obligatoire pour exécuter le MVP local.

`.env.example` documente au minimum `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DATABASE_URL`, `APP_BASE_URL`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, `SPRING_PROFILES_ACTIVE`. Le README explique le chargement explicite de ces variables et leur correspondance Spring. Les variables frontend `VITE_*` ne contiennent jamais de secrets.

Démarrage intégral attendu depuis la racine :

```sh
cp .env.example .env
make dev
```

`make dev` doit démarrer PostgreSQL et un serveur mail local de capture (par exemple Mailpit), attendre les healthchecks, migrer la base, importer les contenus, démarrer backend et frontend. Il ne doit jamais écraser une base existante. URL locales attendues : frontend `http://localhost:5173`, API `http://localhost:8080`, boîte e-mail `http://localhost:8025`. Swagger local via route documentée ; pas d'exposition publique non contrôlée en production.

Mode manuel à documenter et implémenter :

```sh
docker compose up -d postgres mailpit
make backend-dev
make content-import
make frontend-dev
```

Les deux commandes de développement démarrent des processus longs dans des terminaux distincts ; `content-import` s'exécute après migration de la base. Les cibles Make chargent la configuration de façon documentée, sans supposer que Maven lit automatiquement `.env`.

Vérification et arrêt attendus :

```sh
make test
make test-e2e
make lint
make build
make stop
```

`make test` englobe backend, frontend et validation du contenu ; Testcontainers nécessite Docker. `make test-e2e` démarre ou utilise un environnement de test isolé et réinitialisable sans effacer les données de développement. `make stop` arrête les services sans supprimer les volumes. Une éventuelle commande de réinitialisation destructive est séparée, clairement nommée et demande confirmation.

Prévoir `/actuator/health` avec statut minimal ; détails techniques réservés à l'exploitation. La CI doit prouver qu'une base vierge et un clone neuf peuvent suivre le README.

## 18. Déploiement et exploitation

Images multi-stage, processus non root lorsque possible, reverse proxy TLS, assets locaux, migrations contrôlées, healthchecks et readiness. Séparer les profils dev/test/prod ; l'application production refuse les secrets manquants ou connus comme valeurs de démonstration. Pas de console de base ou boîte mail de développement publiée.

Documentation des sauvegardes, restauration, réapplication des suppressions, rotation des secrets, mises à jour, incident de sécurité et retour arrière. Les migrations destructives exigent une stratégie explicite ; restaurer une ancienne image ne suffit pas à annuler une migration de base. Aucun déploiement public, ouverture de compte tiers payant ou publication de données réelles n'est demandé par ce fichier sans instruction du porteur du projet.

## 19. Livrables attendus du développement

- Dépôt complet frontend/backend, migrations, contenus et assets autorisés avec leurs licences.
- MVP local réellement jouable sur Maths et Anglais ; quatre autres mondes signalés futurs.
- README de démarrage depuis un clone vierge, `.env.example`, Compose, Makefile, wrappers et versions verrouillées.
- Contrat OpenAPI, schéma relationnel, documentation d'architecture et des règles de score.
- Soixante exercices corrigés, fichier d'import et guide d'ajout/versionnement.
- Tests et CI, rapport des commandes exécutées, résultats, échecs et limites restantes.
- Notice adulte/enfant à compléter avec l'identité de l'exploitant ; registre proposé des finalités et durées ; procédure d'export/suppression et d'exploitation.
- Liste des décisions, écarts à cette spécification et points nécessitant validation pédagogique/juridique.
- Captures des écrans principaux et démonstration du mode sans WebGL.

La définition de terminé exige des fonctionnalités réelles reliées à PostgreSQL. Un écran statique, une API simulée ou un test simplement écrit ne peut être présenté comme une fonction livrée ou un test réussi.

## 20. Prompt de réalisation pour Claude Code

Copier le texte suivant dans Claude Code avec ce fichier à la racine du dépôt :

> Développe le MVP EduQuest CM1 décrit dans `EDUQUEST_CM1_SPECIFICATIONS.md`. Considère ce fichier comme le contrat fonctionnel et technique. Le projet n'est pas supposé déjà développé : inspecte d'abord le dépôt et indique ce qui existe réellement.
>
> Utilise React TypeScript, Spring Boot Java 21 et PostgreSQL. Réalise un monolithe modulaire et un monorepo. Livre deux mondes jouables, 12 missions et 60 exercices, une carte 3D légère avec alternative HTML/2D, un espace parent, les sessions enfant limitées, la correction serveur, la progression, les récompenses et la gestion des données personnelles.
>
> Commence par un plan court, les versions choisies et les décisions d'architecture. Implémente ensuite des tranches verticales testables : infrastructure et migrations ; authentification/famille ; catalogue/import ; une mission complète persistée ; progression et concurrence ; corpus complet ; interfaces et 3D ; suivi/confidentialité ; tests et documentation. Poursuis jusqu'aux critères MVP, sans remplacer le backend ou les contenus par des simulations définitives.
>
> Applique strictement les autorisations côté serveur et les règles d'idempotence. Ne livre aucun corrigé avant réponse dans les ressources enfant. Ne fais pas confiance aux scores client. N'expose aucun secret. Pas de publicité, achat, classement public, chat ou IA de correction. Respecte la minimisation des données de mineurs et documente les validations encore nécessaires avant publication.
>
> Ajoute les commandes Make prévues et vérifie le démarrage depuis une base vierge. Exécute les tests adaptés, dont PostgreSQL/Testcontainers et les parcours E2E. Rapporte exactement ce qui a été exécuté et les blocages ; ne prétends pas qu'un test passe sans l'avoir exécuté. Documente toute divergence et son impact. Ne déploie pas publiquement sans instruction explicite.
>
> Termine par les livrables, les commandes pour démarrer et tester, les comptes fictifs locaux s'ils existent, les limites connues et une correspondance entre critères d'acceptation et preuves. Le code sera ensuite revu par Codex.

## 21. Checklist de revue pour Codex

### Fonctionnel et pédagogie

- [ ] Deux mondes jouables, six visibles ; les quatre futurs sont inaccessibles côté API.
- [ ] Douze missions et soixante vrais exercices, bonne répartition et corrections relues.
- [ ] Progression, indices, reprise, étoiles, XP et badges conformes aux exemples.
- [ ] Rang d'aventurier distinct des niveaux pédagogiques ; récompense à chaque rang atteint.
- [ ] Rejouer ne crée pas de gain illimité ; une amélioration attribue seulement le delta.
- [ ] Les indicateurs de compétence affichent effectifs et recours aux indices.

### Sécurité et confidentialité

- [ ] Toutes les routes ont une politique d'autorisation ; tests inter-familles probants.
- [ ] Session enfant incapable d'accéder aux routes adultes ; retour parent réauthentifié.
- [ ] Cookies, rotation, expiration absolue, CSRF, récupération et rate limit vérifiés.
- [ ] Aucune solution dans le bundle/catalogue ; correction uniquement après soumission autorisée.
- [ ] Réponses, mots de passe, jetons et données personnelles exclus des logs.
- [ ] Export complet sans secrets ; suppression active et traitement des sauvegardes documentés.
- [ ] Purges réellement exécutables et testées ; aucune donnée enfant superflue.
- [ ] Bases légales et information mineurs cadrées ; absence de prétendue conformité automatique.
- [ ] Aucun compte démo ni secret local utilisable en production.

### Intégrité et architecture

- [ ] Migrations sur base vierge ; contraintes et index présents, pas seulement des validations UI.
- [ ] Versions publiées immuables, historique conservé, réimport idempotent.
- [ ] Clôture et récompenses transactionnelles ; concurrence et doubles envois testés.
- [ ] Verrous pris dans un ordre cohérent ; aucun total XP divergent entre tables.
- [ ] API et DTO typés conformes à OpenAPI ; erreurs non sensibles.
- [ ] Pas de surarchitecture ni dépendance à un service payant pour le local.

### Interface et livraison

- [ ] Clavier, focus, zoom, contrastes, lecteurs d'écran et animations réduites vérifiés.
- [ ] Mode sans WebGL complet, états réseau et session expirée gérés.
- [ ] Démarrage neuf reproductible avec les commandes documentées.
- [ ] Tests réellement exécutés, preuves de résultats et limites explicites.
- [ ] Assets locaux licenciés, dépendances verrouillées et CI utilisable.
- [ ] Aucune fonction simplement simulée présentée comme terminée.

Pour chaque anomalie, Codex doit indiquer le fichier et la ligne vérifiés, un scénario reproductible, l'impact et une correction proposée. Priorité P0 : fuite de données, élévation de privilège ou corruption critique ; P1 : parcours principal cassé ou score/reprise incorrect ; P2 : défaut limité ; P3 : amélioration mineure. La revue distingue les défauts établis des risques à confirmer et signale les tests non exécutés.

## 22. Décisions et validations avant ouverture publique

Les choix par défaut du MVP sont arrêtés dans ce document pour permettre le développement : usage familial, comptes parents, sessions enfants, deux mondes, 60 exercices, grille d'XP, 3D à caméra fixe et contenus versionnés. Ne pas attendre les choix suivants pour construire et tester localement avec des données fictives.

Avant publication réelle, obtenir : validation du corpus par un enseignant de CM1 ; identité et coordonnées de l'exploitant ; choix d'hébergement et d'envoi d'e-mails ; validation des finalités/bases légales et du mécanisme d'accord adapté aux enfants ; analyse des risques/AIPD si nécessaire ; validation des durées et de la procédure d'incident ; recette avec enfants accompagnés et vérification d'accessibilité. Consigner les décisions et modifier les documents concernés sans prétendre que ces validations ont déjà eu lieu.
