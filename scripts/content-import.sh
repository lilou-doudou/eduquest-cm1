#!/usr/bin/env bash
# Applique les migrations Flyway puis importe content/ (idempotent). Application lancée sans serveur web.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"
./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --eduquest.content.import=true --eduquest.content.path=$ROOT/content --eduquest.dev-seed=false"
