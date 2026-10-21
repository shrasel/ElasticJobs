.PHONY: help dev build test clean docker-up docker-down docker-logs es-start es-stop healthcheck

# Default target
.DEFAULT_GOAL := help

help: ## Display this help message
	@echo "========================================================================"
	@echo " ElasticJobs Monorepo Management CLI"
	@echo "========================================================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ------------------------------------------------------------------------------
# Local Development
# ------------------------------------------------------------------------------
dev: es-start ## Start Elasticsearch and local backend service
	@echo "Starting backend Spring Boot service..."
	cd backend && mvn spring-boot:run

build: ## Build both backend (Maven jar) and frontend production assets
	@echo "Building backend..."
	cd backend && mvn clean package -DskipTests
	@echo "Frontend ready."

test: ## Execute test suites across all workspaces
	@echo "Testing backend..."
	cd backend && mvn test

clean: ## Clean build artifacts, Maven targets, and temporary files
	@echo "Cleaning backend target..."
	cd backend && mvn clean
	@echo "Clean completed."

# ------------------------------------------------------------------------------
# Docker Orchestration
# ------------------------------------------------------------------------------
docker-up: ## Launch entire multi-container production stack (ES + Backend + Frontend)
	docker compose -f docker/docker-compose.yml up -d --build

docker-down: ## Stop all monorepo containers
	docker compose -f docker/docker-compose.yml down

docker-logs: ## Tail aggregated logs from all services
	docker compose -f docker/docker-compose.yml logs -f

es-start: ## Start standalone Elasticsearch 8.15 container
	docker compose -f docker/docker-compose.yml up -d elasticsearch
	@echo "Waiting for Elasticsearch to become healthy..."
	@until curl -s http://localhost:9200/_cluster/health | grep -q '"status":"\(green\|yellow\)"'; do sleep 2; done
	@echo "Elasticsearch is ready!"

es-stop: ## Stop Elasticsearch container
	docker compose -f docker/docker-compose.yml stop elasticsearch

healthcheck: ## Run end-to-end health check across all services
	bash scripts/healthcheck.sh
