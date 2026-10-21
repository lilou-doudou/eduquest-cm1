#!/usr/bin/env bash
# Charge .env (ou .env.example à défaut) puis exécute la commande donnée.
# Spring lit ensuite ces variables d'environnement via les placeholders de application.yml
# (DATABASE_URL → spring.datasource.url, SMTP_HOST → spring.mail.host, etc.).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"
[ -f "$ENV_FILE" ] || { echo "⚠ .env absent : utilisation de .env.example (cp .env.example .env pour personnaliser)" >&2; ENV_FILE="$ROOT/.env.example"; }
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a
exec "$@"
