#!/usr/bin/env bash
# make dev : services Docker (healthchecks), migration + import des contenus, puis backend et frontend.
# Ne supprime jamais le volume PostgreSQL existant. Ctrl+C arrête backend et frontend.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

docker compose up -d --wait postgres mailpit
"$ROOT/scripts/content-import.sh"

(cd backend && ./mvnw -q spring-boot:run) &
BACKEND=$!
(cd frontend && { [ -d node_modules ] || npm ci; } && npm run dev) &
FRONTEND=$!
trap 'kill $BACKEND $FRONTEND 2>/dev/null || true; wait 2>/dev/null' INT TERM EXIT

echo
echo "  Frontend : http://localhost:5173"
echo "  API      : http://localhost:8080 (Swagger en profil dev : http://localhost:8080/swagger-ui.html)"
echo "  E-mails  : http://localhost:8025"
echo
wait
