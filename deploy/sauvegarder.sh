#!/usr/bin/env bash
# Copie compressée de la base dans /var/backups/eduquest, 14 jours conservés.
# Restauration (DESTRUCTIF pour les données actuelles) : voir docs/mise-en-ligne.md.
set -euo pipefail
cd "$(dirname "$0")"
dest=/var/backups/eduquest
mkdir -p "$dest"
chmod 700 "$dest"
file="$dest/eduquest-$(date -u +%Y%m%d-%H%M%S).sql.gz"
docker compose --env-file prod.env -f compose.yaml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner' | gzip > "$file"
chmod 600 "$file"
find "$dest" -name 'eduquest-*.sql.gz' -mtime +14 -delete
echo "Sauvegarde : $file"
