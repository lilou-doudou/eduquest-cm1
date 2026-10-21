# Mettre EduQuest en ligne (guide pas à pas)

Sur ton Mac, l'application tourne « en local » : seule ta machine peut l'ouvrir (`http://localhost:3000`). Pour qu'elle soit visible depuis Internet, il faut la faire tourner sur un **serveur** allumé en permanence, avec une **adresse web** (nom de domaine) et le **HTTPS**. Ce guide t'y emmène en une heure environ, sans rien programmer.

## Ce qu'il te faut et combien ça coûte

| Élément | Rôle | Proposition | Coût indicatif |
|---|---|---|---|
| Serveur (VPS) | Ordinateur en ligne 24 h/24 qui fait tourner l'application | **Hetzner Cloud**, modèle partagé x86 le moins cher avec **4 Go de RAM** (gamme CX), en Allemagne ou Finlande | environ 4 à 5 € / mois |
| Nom de domaine | L'adresse du site, par ex. `eduquest-famille.fr` | OVHcloud, Gandi, Infomaniak… | environ 7 à 15 € / an |
| Envoi d'e-mails | Validation des comptes parents, mot de passe oublié | **Brevo** (entreprise française), offre gratuite | 0 € (300 e-mails / jour) |
| Sauvegardes du serveur (conseillé) | Copie complète du serveur chaque jour | option « Backups » de Hetzner | + 20 % du prix du serveur |

**Total : environ 6 € par mois.** Vérifie les prix sur les sites au moment de commander, ils changent de temps en temps.

Alternative équivalente : un VPS **OVHcloud** (serveurs en France, environ 5 à 7 € / mois). Toute la suite du guide fonctionne pareil, seule l'étape 2 change d'écran.

Pourquoi ce choix : un seul petit serveur en Europe fait tourner l'application exactement comme sur ton Mac (mêmes conteneurs Docker), le HTTPS est automatique et gratuit, et les données des enfants restent dans l'Union européenne.

## Ce qui est déjà prêt dans le dépôt

Le dossier `deploy/` contient tout le nécessaire, tu n'as rien à modifier :

| Fichier | Rôle |
|---|---|
| `deploy/installer-serveur.sh` | Prépare un serveur neuf : Docker, pare-feu, mises à jour de sécurité automatiques, mot de passe de base tiré au hasard, sauvegarde quotidienne |
| `deploy/demarrer.sh` | Construit et démarre l'application ; le relancer plus tard installe la dernière version |
| `deploy/compose.yaml` + `deploy/Caddyfile` | Application en mode **production** derrière Caddy, qui obtient le certificat HTTPS tout seul |
| `deploy/sauvegarder.sh` / `deploy/restaurer.sh` | Sauvegarde de la base (automatique chaque nuit) et restauration |

Sécurité en production, garantie par l'application elle-même : **aucun compte de démonstration**, aucun mot de passe par défaut (le serveur refuse de démarrer sinon), HTTPS obligatoire, base de données jamais exposée sur Internet, Mailpit absent. Chaque parent crée son propre compte et le valide par e-mail.

---

## Étape 1 : créer une clé SSH sur ton Mac (2 min)

La clé SSH sert à te connecter au serveur sans mot de passe. Ouvre l'app **Terminal** et tape :

```sh
ssh-keygen -t ed25519
```

Appuie sur Entrée à chaque question (tu peux choisir une phrase de passe, c'est mieux). Puis copie la partie publique dans le presse-papiers :

```sh
pbcopy < ~/.ssh/id_ed25519.pub
```

## Étape 2 : louer le serveur (10 min)

1. Crée un compte sur [hetzner.com/cloud](https://www.hetzner.com/cloud) (une vérification d'identité peut être demandée).
2. Dans la console, crée un projet « EduQuest », puis **Add Server** :
   - **Location** : Nuremberg, Falkenstein ou Helsinki (Union européenne) ;
   - **Image** : Ubuntu 24.04 ;
   - **Type** : Shared vCPU, x86, le moins cher avec **4 Go de RAM** ;
   - **SSH keys** : « Add SSH key », colle (Cmd+V) ce que tu as copié à l'étape 1 ;
   - **Backups** : coche-le si tu veux la copie quotidienne du serveur ;
   - nom : `eduquest`, puis **Create & Buy now**.
3. Note l'**adresse IPv4** affichée (par ex. `203.0.113.10`).

## Étape 3 : acheter le nom de domaine et le relier au serveur (10 min, puis attente)

1. Achète un domaine chez OVHcloud, Gandi ou Infomaniak.
2. Dans la **zone DNS** du domaine, crée (ou modifie) un enregistrement :
   - type **A**, sous-domaine vide (ou `@`), valeur : l'adresse IPv4 du serveur.
   - Si tu préfères une adresse comme `jeu.mondomaine.fr`, mets `jeu` comme sous-domaine.
3. Attends que ce soit pris en compte (de quelques minutes à quelques heures). Pour vérifier depuis le Terminal du Mac :
   ```sh
   nslookup eduquest-famille.fr
   ```
   Quand l'adresse du serveur apparaît dans la réponse, c'est bon.

## Étape 4 : préparer l'envoi des e-mails avec Brevo (15 min)

1. Crée un compte gratuit sur [brevo.com](https://www.brevo.com).
2. Menu **Expéditeurs, domaines et IP dédiées** → **Domaines** → ajoute ton domaine, puis copie les enregistrements DNS qu'il te donne (DKIM, DMARC…) dans la zone DNS de ton domaine, comme à l'étape 3. Clique sur « Authentifier ».
3. Ajoute un **expéditeur** avec une adresse de ton domaine, par ex. `no-reply@eduquest-famille.fr`.
4. Menu **SMTP et API** → onglet **SMTP** → **Générer une nouvelle clé SMTP**. Note :
   - le serveur : `smtp-relay.brevo.com`, port `587` ;
   - l'**identifiant** (« Connexion », une adresse qui finit souvent par `@smtp-brevo.com`) ;
   - la **clé SMTP** (elle ne s'affiche qu'une fois).

## Étape 5 : installer l'application sur le serveur (20 min)

Depuis le Terminal du Mac, connecte-toi au serveur (remplace par ton adresse IP) :

```sh
ssh root@203.0.113.10
```

Réponds `yes` la première fois. Tu es maintenant **sur le serveur**. Copie-colle ces commandes :

```sh
git clone https://github.com/lilou-doudou/eduquest-cm1.git /opt/eduquest
cd /opt/eduquest
./deploy/installer-serveur.sh
```

Le script te pose six questions : ton nom de domaine, puis les informations Brevo de l'étape 4 (serveur SMTP, port, identifiant, clé, adresse d'expédition). Il crée le fichier secret `deploy/prod.env` et invente lui-même un mot de passe solide pour la base de données.

Puis lance l'application :

```sh
./deploy/demarrer.sh
```

La première fois, la construction prend 5 à 10 minutes. À la fin, le script affiche :

```
EduQuest est en ligne : https://eduquest-famille.fr
```

Ouvre cette adresse sur ton téléphone (en 4G, pas en Wi-Fi) pour vérifier qu'elle est bien visible de l'extérieur, crée un compte parent et vérifie que l'e-mail de validation arrive.

Pour te déconnecter du serveur : `exit`.

## Utilisation au quotidien

| Je veux… | Commande (après `ssh root@IP` puis `cd /opt/eduquest`) |
|---|---|
| Installer la dernière version du dépôt | `./deploy/demarrer.sh` (sauvegarde la base avant) |
| Voir si tout tourne | `docker compose --env-file deploy/prod.env -f deploy/compose.yaml ps` |
| Lire les journaux du serveur | `docker compose --env-file deploy/prod.env -f deploy/compose.yaml logs --tail 100 backend` |
| Faire une sauvegarde tout de suite | `./deploy/sauvegarder.sh` |
| Lister les sauvegardes | `ls -lh /var/backups/eduquest` |
| Restaurer une sauvegarde (efface les données actuelles) | `./deploy/restaurer.sh /var/backups/eduquest/<fichier>` |
| Changer un réglage (e-mail…) | `nano deploy/prod.env` puis `./deploy/demarrer.sh --local` |

Les sauvegardes de `/var/backups/eduquest` sont sur le même serveur : l'option « Backups » de Hetzner (ou une copie régulière sur ton Mac avec `scp root@IP:/var/backups/eduquest/* .`) protège aussi contre la perte du serveur.

## En cas de problème

- **Le site ne s'ouvre pas / erreur de certificat** : le domaine ne pointe pas encore vers le serveur (refais le `nslookup` de l'étape 3). Une fois le DNS correct, relance `./deploy/demarrer.sh --local`.
- **« Le backend n'a pas démarré à temps »** : lis les journaux (tableau ci-dessus). Un message `POSTGRES_PASSWORD` ou `APP_BASE_URL` signifie que `deploy/prod.env` est incomplet.
- **Les e-mails n'arrivent pas** : vérifie l'identifiant et la clé SMTP dans `deploy/prod.env`, que le domaine est bien « authentifié » chez Brevo, et regarde les courriers indésirables.

## Avant d'ouvrir le site à d'autres familles

L'application traite des données d'enfants. Pour un test en famille ou entre amis, tout est prêt. Avant de la faire connaître plus largement, publie des mentions légales et une politique de confidentialité (voir `docs/security-privacy.md`) et relis `docs/deployment.md` (procédures d'exploitation).

## Juste montrer l'application quelques minutes depuis ton Mac ?

Sans louer de serveur, un « tunnel » donne une adresse publique temporaire vers ton Mac. Avec l'application lancée (`docker compose --profile app up`) :

```sh
brew install cloudflared
cloudflared tunnel --url http://localhost:3000
```

Une adresse `https://…trycloudflare.com` s'affiche ; elle marche tant que le Mac reste allumé et la commande ouverte (Ctrl+C pour couper). Attention : c'est le **mode développement**, avec le compte de démonstration et sans vrais e-mails (les nouveaux comptes ne peuvent pas être validés). À réserver à une démonstration courte, jamais à de vrais enfants.
