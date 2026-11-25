# Ressources visuelles et licences

Toutes les ressources visuelles du MVP sont **originales au projet** et produites par du code ; aucune image, police, modèle 3D ou son tiers n'est embarqué.

| Ressource | Origine | Licence |
|---|---|---|
| Îles de la carte 3D (`world-*`) | Géométries Three.js générées dans `frontend/src/scene/WorldScene3D.tsx` | Celle du dépôt |
| Carte 2D | SVG inline, `frontend/src/scene/WorldMap2D.tsx` | Celle du dépôt |
| Avatars (renard, hibou, tortue, chat, panda, lapin) et accessoires | SVG inline, `frontend/src/components/Avatar.tsx` | Celle du dépôt |
| Icônes de badges et d'accessoires | SVG inline, `frontend/src/components/RewardIcon.tsx` | Celle du dépôt |
| Favicon | `frontend/public/assets/favicon.svg` | Celle du dépôt |
| Police des titres et boutons | « Baloo 2 » (graisses 600, 700, 800, sous-ensemble latin), auto-hébergée via le paquet npm `@fontsource/baloo-2` : aucune requête vers un service tiers, compatible avec la CSP `default-src 'self'` | SIL Open Font License 1.1 |
| Police du texte | Aucune police téléchargée : « Atkinson Hyperlegible » est utilisée seulement si elle est déjà installée sur l'appareil, sinon la pile système (`Segoe UI`, `system-ui`…) | — |
| Sons de correction | Synthétisés par Web Audio (`frontend/src/app/preferences.ts`), désactivés par défaut | Celle du dépôt |

L'inventaire machine est `content/assets-manifest.json` (clé, type, licence) ; le validateur de contenus vérifie que chaque `assetKey` des mondes et récompenses y figure.

La licence du dépôt lui-même n'a pas encore été choisie par le porteur du projet (aucun fichier `LICENSE`). Tout ajout futur de ressource tierce devra figurer dans le manifeste avec sa source et sa licence, et être servi localement (la CSP n'autorise aucune origine externe).

## Dépendances logicielles

Les bibliothèques (npm et Maven) gardent leurs licences propres ; les principales sont MIT (React, Three.js, @react-three/fiber, TanStack Query, Vite, Playwright est Apache-2.0) ou Apache-2.0 (Spring, Hibernate, Flyway Community, springdoc), PostgreSQL JDBC (BSD-2) et BouncyCastle (MIT-like). Un audit de licences outillé (par exemple `license-checker` et `license-maven-plugin`) reste à ajouter à la CI.
