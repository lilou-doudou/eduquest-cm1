#!/usr/bin/env bash
# DESTRUCTIF : remplace toute la base actuelle par une sauvegarde.
#   sudo ./deploy/restaurer.sh /var/backups/eduquest/eduquest-AAAAMMJJ-HHMMSS.sql.gz
# Ensuite, réappliquer les suppressions de comptes faites depuis la date de la sauvegarde (docs/deployment.md).
set -euo pipefail
cd "$(dirname "$0")"
file=${1:?Indique le fichier de sauvegarde à restaurer (voir ls /var/backups/eduquest)}
[ -f "$file" ] || { echo "Fichier introuvable : $file" >&2; exit 1; }

read -rp "Toutes les données actuelles seront remplacées par $file. Écris RESTAURER pour confirmer : " answer
[ "$answer" = "RESTAURER" ] || { echo "Annulé."; exit 1; }

compose() { docker compose --env-file prod.env -f compose.yaml "$@"; }
psql_db() { compose exec -T postgres sh -c "psql -q -v ON_ERROR_STOP=1 -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" $*"; }

./sauvegarder.sh
compose stop backend
psql_db '-c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'
gunzip -c "$file" | psql_db
compose start backend
echo "Restauration terminée."
