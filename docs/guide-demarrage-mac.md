# Lancer EduQuest CM1 sur un Mac (puce Apple M1/M2/M3)

Ce guide ne demande que **Docker Desktop**, **VS Code** et un compte GitHub. Java et Node ne sont pas nécessaires : tout tourne dans des conteneurs (images compatibles puces Apple).

## 1. Démarrer Docker Desktop

1. Ouvre **Docker Desktop** (Launchpad ou `Cmd + Espace`, tape « Docker »).
2. Attends que l'icône de baleine dans la barre de menus en haut de l'écran ne bouge plus et que la fenêtre affiche « Engine running » (en bas à gauche). La première fois, accepte les conditions et ignore la connexion à un compte Docker : elle n'est pas nécessaire.

Laisse Docker Desktop ouvert pendant toute l'utilisation du jeu.

## 2. Cloner le projet avec VS Code

1. Ouvre **VS Code**.
2. Appuie sur `Cmd + Maj + P`, tape `Git: Clone` puis Entrée.
3. Choisis **Clone from GitHub**, accepte la connexion à GitHub dans le navigateur (compte `lilou-doudou`), puis choisis `lilou-doudou/eduquest-cm1`.
   - Si macOS propose d'installer les « outils de ligne de commande » (nécessaires pour Git), accepte, attends la fin de l'installation, puis recommence l'étape 2.
4. Choisis un dossier (par exemple `Documents`), puis clique sur **Open** quand VS Code propose d'ouvrir le projet. Si VS Code demande « Do you trust the authors? », réponds **Yes**.

## 3. Ouvrir un terminal dans VS Code

Menu **Terminal → New Terminal**. Un terminal s'ouvre en bas, déjà placé dans le dossier du projet. Vérifie que Docker répond :

```sh
docker compose version
```

Une ligne du type `Docker Compose version v2…` doit s'afficher. Si tu lis « command not found » ou « Cannot connect to the Docker daemon », Docker Desktop n'est pas démarré (étape 1).

## 4. Créer le fichier de configuration

Copie le modèle fourni (à faire une seule fois) :

```sh
cp .env.example .env
```

## 5. Lancer le jeu

```sh
docker compose --profile app up --build
```

La première fois, Docker télécharge et construit tout : compte **5 à 10 minutes**. Beaucoup de texte défile, c'est normal. C'est prêt quand tu vois des lignes `web-1` et que plus rien ne bouge.

Ouvre ensuite ton navigateur :

| Adresse | Contenu |
|---|---|
| http://localhost:3000 | Le jeu |
| http://localhost:8025 | La boîte e-mail de test (liens de confirmation d'inscription) |

## 6. Se connecter

Un compte de démonstration existe déjà :

- e-mail : `parent.demo@eduquest.local`
- mot de passe : `demo-eduquest-local`

Après connexion, choisis **Nova** ou **Tiago** dans « Qui joue ? », puis clique sur une île. Pour revenir à l'espace adulte depuis le mode enfant : **Paramètres → Espace adulte**, avec le même e-mail et le même mot de passe.

Tu peux aussi créer ton propre compte avec « Créer un compte » : le lien de confirmation arrive sur http://localhost:8025.

## 7. Arrêter et relancer

- **Arrêter** : clique dans le terminal et appuie sur `Ctrl + C`, puis tape :
  ```sh
  docker compose --profile app down
  ```
  Les parties et les comptes sont conservés.
- **Relancer plus tard** (rapide, sans reconstruire) :
  ```sh
  docker compose --profile app up
  ```
- **Après avoir récupéré une nouvelle version du code** (VS Code : bouton « Sync » ou `git pull`), ajoute `--build` pour reconstruire.
- **Tout effacer et repartir de zéro** (comptes et parties supprimés) :
  ```sh
  docker compose --profile app down -v
  ```

## En cas de problème

| Message | Que faire |
|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop n'est pas démarré : étape 1, puis relance la commande. |
| `port is already allocated` ou `address already in use` sur **5432** | Un PostgreSQL tourne déjà sur ton Mac. Ouvre le fichier `.env` dans VS Code et ajoute (ou modifie) la ligne `POSTGRES_HOST_PORT=55432`, enregistre (`Cmd + S`), puis relance `docker compose --profile app down` et `docker compose --profile app up --build`. Le jeu n'en est pas affecté. |
| Même message sur 3000, 8025 ou 1025 | Même principe dans `.env` : `APP_PORT=3001` (le jeu sera alors sur http://localhost:3001), `MAILPIT_UI_PORT=8026` ou `SMTP_HOST_PORT=1026`. Une ancienne exécution peut aussi occuper le port : `docker compose --profile app down`. |
| `POSTGRES_PASSWORD` manquant | Le fichier `.env` n'existe pas : étape 4. |
| La page reste blanche ou « Chargement… » longtemps | Attends la fin du démarrage, puis recharge la page (`Cmd + R`). |
| La construction échoue faute de mémoire | Docker Desktop → **Settings → Resources** : mets au moins 4 Go de mémoire. |

Pour développer (modifier le code avec rechargement automatique), il faut en plus Java 21 et Node 22 : voir le [README](../README.md).
