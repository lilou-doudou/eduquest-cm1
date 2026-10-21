#!/usr/bin/env bash
# Construit et (re)démarre EduQuest en production. Sert aussi aux mises à jour :
#   ./deploy/demarrer.sh          → récupère la dernière version du dépôt, reconstruit, redémarre
#   ./deploy/demarrer.sh --local  → reconstruit sans git pull
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f prod.env ]; then
  echo "deploy/prod.env manquant : lance d'abord sudo ./deploy/installer-serveur.sh" >&2
  exit 1
fi

if [ "${1:-}" != "--local" ]; then
  echo "==> Récupération de la dernière version"
  git -C .. pull --ff-only
fi

compose() { docker compose --env-file prod.env -f compose.yaml "$@"; }

echo "==> Sauvegarde de la base avant mise à jour (si elle existe déjà)"
if compose ps --status running postgres 2>/dev/null | grep -q postgres; then ./sauvegarder.sh; fi

echo "==> Construction des images (quelques minutes la première fois)"
compose build

echo "==> Démarrage"
compose up -d --remove-orphans

echo "==> Attente du démarrage de l'application"
for _ in $(seq 1 60); do
  if compose ps backend | grep -q "(healthy)"; then
    domain=$(grep -E '^DOMAIN=' prod.env | cut -d= -f2)
    echo
    echo "EduQuest est en ligne : https://${domain}"
    exit 0
  fi
  sleep 5
done
echo "Le backend n'a pas démarré à temps. Journaux : docker compose --env-file deploy/prod.env -f deploy/compose.yaml logs backend" >&2
exit 1
