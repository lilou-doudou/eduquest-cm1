#!/usr/bin/env bash
# Prépare un serveur Ubuntu 24.04 neuf pour EduQuest (à lancer une seule fois, en root) :
#   Docker, pare-feu (SSH, HTTP, HTTPS), mises à jour de sécurité automatiques, mémoire d'échange,
#   fichier deploy/prod.env (mot de passe de base tiré au hasard), sauvegarde quotidienne de la base.
set -euo pipefail
cd "$(dirname "$0")"

if [ "$(id -u)" -ne 0 ]; then
  echo "Lance ce script en root : sudo ./deploy/installer-serveur.sh" >&2
  exit 1
fi

echo "==> Mises à jour du système"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get upgrade -yq
apt-get install -yq ca-certificates curl git ufw unattended-upgrades openssl
dpkg-reconfigure -f noninteractive unattended-upgrades

if ! command -v docker >/dev/null; then
  echo "==> Installation de Docker"
  curl -fsSL https://get.docker.com | sh
fi

echo "==> Pare-feu : seuls SSH, HTTP et HTTPS sont ouverts"
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

if ! swapon --show | grep -q .; then
  echo "==> Mémoire d'échange de 2 Go (utile pendant la construction des images)"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

if [ ! -f prod.env ]; then
  echo "==> Configuration (deploy/prod.env)"
  read -rp "Nom de domaine du site (ex. eduquest-famille.fr) : " domain
  read -rp "Serveur SMTP [smtp-relay.brevo.com] : " smtp_host
  read -rp "Port SMTP [587] : " smtp_port
  read -rp "Identifiant SMTP : " smtp_user
  read -rsp "Mot de passe / clé SMTP (rien ne s'affiche) : " smtp_pass; echo
  read -rp "Adresse d'expédition validée chez le prestataire (ex. no-reply@${domain}) : " mail_from
  umask 077
  cat > prod.env <<CONF
DOMAIN=${domain}
POSTGRES_DB=eduquest
POSTGRES_USER=eduquest
POSTGRES_PASSWORD=$(openssl rand -hex 24)
SMTP_HOST=${smtp_host:-smtp-relay.brevo.com}
SMTP_PORT=${smtp_port:-587}
SMTP_USERNAME=${smtp_user}
SMTP_PASSWORD='${smtp_pass}'
MAIL_FROM="EduQuest CM1 <${mail_from}>"
CONF
  echo "    deploy/prod.env créé (lisible par root uniquement). Modifiable avec : nano deploy/prod.env"
fi

echo "==> Sauvegarde quotidienne de la base à 03:00 (14 jours conservés dans /var/backups/eduquest)"
cat > /etc/cron.d/eduquest-sauvegarde <<CRON
0 3 * * * root $(pwd)/sauvegarder.sh >> /var/log/eduquest-sauvegarde.log 2>&1
CRON

echo
echo "Serveur prêt. Étape suivante : ./deploy/demarrer.sh"
