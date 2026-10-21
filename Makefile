# EduQuest CM1 — commandes de développement. Les cibles chargent .env via scripts/env.sh
# (Maven ne lit pas .env tout seul). Voir README.md.
SHELL := /bin/bash
ENV := ./scripts/env.sh

.PHONY: help dev backend-dev content-import content-validate frontend-dev test test-backend test-frontend \
        test-e2e lint build stop reset-dev-db perf screenshots

help: ## Liste les commandes
	@grep -E '^[a-z0-9-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-18s %s\n", $$1, $$2}'

dev: ## Tout démarrer : PostgreSQL, Mailpit, migrations, contenus, backend (8080), frontend (5173)
	$(ENV) ./scripts/dev.sh

backend-dev: ## Backend Spring Boot seul (processus long)
	$(ENV) bash -c 'cd backend && ./mvnw spring-boot:run'

content-import: ## Migre la base puis importe content/ (idempotent)
	$(ENV) ./scripts/content-import.sh

content-validate: ## Valide content/ contre content/schema sans toucher la base
	cd frontend && npm run --silent content:validate

frontend-dev: ## Frontend Vite seul (processus long)
	cd frontend && { [ -d node_modules ] || npm ci; } && npm run dev

test: content-validate test-backend test-frontend ## Contenus + backend (Testcontainers, Docker requis) + frontend

test-backend:
	cd backend && ./mvnw -B verify

test-frontend:
	cd frontend && { [ -d node_modules ] || npm ci; } && npm run typecheck && npm test

test-e2e: ## Pile isolée (PostgreSQL jetable :5433, API :8081, Vite :5174) + Playwright
	$(ENV) ./scripts/e2e.sh

lint: ## Lint et typecheck frontend, compilation Java stricte
	cd frontend && npm run lint && npm run typecheck
	cd e2e && { [ -d node_modules ] || npm ci; } && npx tsc -p .
	cd backend && ./mvnw -B -q -DskipTests compile

build: ## Jar backend et bundle frontend de production
	cd backend && ./mvnw -B -q -DskipTests package
	cd frontend && npm run build

perf: ## Mesure de charge locale (20 enfants simultanés) contre http://localhost:8080
	node scripts/perf.mjs

screenshots: ## Captures des écrans principaux dans docs/screenshots (pile E2E)
	CAPTURE=1 $(ENV) ./scripts/e2e.sh --project=desktop --project=no-webgl --project=mobile captures

stop: ## Arrête les services sans supprimer les volumes
	docker compose --profile e2e stop

reset-dev-db: ## DESTRUCTIF : supprime la base de développement (demande confirmation)
	@read -p "Supprimer définitivement la base de développement ? Taper SUPPRIMER : " ok; \
	if [ "$$ok" = "SUPPRIMER" ]; then docker compose down -v; else echo "Annulé."; fi
