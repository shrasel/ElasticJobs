#!/usr/bin/env bash
set -eo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================================================${NC}"
echo -e "${BLUE} Bootstrapping ElasticJobs Monorepo Environment${NC}"
echo -e "${BLUE}========================================================================${NC}"

# Check Prerequisites
echo "Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { echo -e "${RED}Error: Docker is required but not installed.${NC}" >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo -e "${YELLOW}Warning: Java 21 not found locally (required if running outside Docker).${NC}" >&2; }
command -v mvn >/dev/null 2>&1 || { echo -e "${YELLOW}Warning: Maven not found locally (required if running outside Docker).${NC}" >&2; }

echo -e "${GREEN}Prerequisites check completed.${NC}"

# Prepare Docker Environment File
if [ ! -f docker/.env ]; then
    echo "Creating docker/.env from template..."
    cp docker/.env.example docker/.env
fi

echo -e "${GREEN}Setup completed successfully!${NC}"
echo ""
echo "Next Steps:"
echo "  1. Start full Docker stack:    make docker-up"
echo "  2. Or run local developer mode: make dev"
echo "  3. Verify service health:      make healthcheck"
echo ""
