#!/usr/bin/env bash
# Lance la pile E2E isolée (ou, avec PERF=1, la mesure de charge scripts/perf.mjs) : PostgreSQL jetable (5433), backend sur 8081, Vite sur 5174, puis Playwright.
# Mailpit (compose, port 8025) reçoit les e-mails de vérification lus par les tests.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export DATABASE_URL="jdbc:postgresql://localhost:5433/eduquest_e2e"
export POSTGRES_USER=eduquest
export POSTGRES_PASSWORD=e2e-only
export APP_BASE_URL="http://127.0.0.1:5174"
export SPRING_PROFILES_ACTIVE=dev
export SMTP_HOST=localhost SMTP_PORT=1025
export MAIL_FROM="EduQuest CM1 <no-reply@eduquest.local>"
LOG_DIR="$ROOT/e2e/.logs"
mkdir -p "$LOG_DIR"

docker compose --profile e2e up -d --wait postgres-e2e mailpit

if [ "${SKIP_BACKEND_BUILD:-0}" != "1" ]; then
  (cd backend && ./mvnw -q -DskipTests package)
fi
JAR="$(ls backend/target/eduquest-backend-*.jar | grep -v plain | head -1)"

(cd backend && java -jar "$ROOT/$JAR" --spring.main.web-application-type=none \
  --eduquest.content.import=true --eduquest.content.path="$ROOT/content") > "$LOG_DIR/import.log" 2>&1

java -jar "$JAR" --server.port=8081 --eduquest.rate-limit.ip-factor=50 \
  --eduquest.content.path="$ROOT/content" > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
cleanup() {
  kill "$BACKEND_PID" 2>/dev/null || true
  if [ "${KEEP_E2E_DB:-0}" != "1" ]; then docker compose --profile e2e stop postgres-e2e >/dev/null; fi
}
trap cleanup EXIT

for _ in $(seq 1 60); do
  curl -sf http://127.0.0.1:8081/actuator/health >/dev/null && break
  sleep 1
done
curl -sf http://127.0.0.1:8081/actuator/health >/dev/null || { echo "Le backend E2E n'a pas démarré (voir $LOG_DIR/backend.log)"; exit 1; }

if [ "${PERF:-0}" = "1" ]; then
  API_URL=http://127.0.0.1:8081 node scripts/perf.mjs
  exit $?
fi
[ -d e2e/node_modules ] || (cd e2e && npm ci)
cd e2e && npx playwright test "$@"
