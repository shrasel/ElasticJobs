#!/usr/bin/env bash
set -eo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================================================${NC}"
echo -e "${BLUE} ElasticJobs Monorepo Health Check${NC}"
echo -e "${BLUE}========================================================================${NC}"

# 1. Elasticsearch Check
echo -ne "1. Checking Elasticsearch (http://localhost:9200)... "
if ES_STATUS=$(curl -s http://localhost:9200/_cluster/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4); then
    if [[ "$ES_STATUS" == "green" || "$ES_STATUS" == "yellow" ]]; then
        echo -e "${GREEN}HEALTHY ($ES_STATUS)${NC}"
    else
        echo -e "${RED}UNHEALTHY ($ES_STATUS)${NC}"
    fi
else
    echo -e "${RED}OFFLINE${NC}"
fi

# Document Count Check
echo -ne "   Checking Document Count in 'jobs' index... "
DOC_COUNT=$(curl -s http://localhost:9200/jobs/_count 2>/dev/null | grep -o '"count":[0-9]*' | cut -d':' -f2 || echo "0")
echo -e "${GREEN}${DOC_COUNT} jobs indexed${NC}"

# 2. Spring Boot Backend API Check
echo -ne "2. Checking Spring Boot Backend (http://localhost:8090/v3/api-docs)... "
if HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/v3/api-docs 2>/dev/null); then
    if [[ "$HTTP_CODE" == "200" ]]; then
        echo -e "${GREEN}UP (HTTP 200)${NC}"
    else
        echo -e "${YELLOW}HTTP $HTTP_CODE${NC}"
    fi
else
    echo -e "${RED}OFFLINE${NC}"
fi

# 3. Frontend Web App Check
echo -ne "3. Checking Frontend Web Application... "
if curl -s -I http://localhost:8080/ 2>/dev/null | grep -q "200 OK"; then
    echo -e "${GREEN}UP on Port 8080 (NGINX container)${NC}"
elif curl -s -I http://localhost:3000/ 2>/dev/null | grep -q "200 OK"; then
    echo -e "${GREEN}UP on Port 3000 (NGINX container)${NC}"
elif curl -s -I http://localhost/ 2>/dev/null | grep -q "200 OK"; then
    echo -e "${GREEN}UP on Port 80 (NGINX container)${NC}"
elif curl -s -I http://localhost:8090/ 2>/dev/null | grep -q "200"; then
    echo -e "${GREEN}UP on Port 8090 (Spring Boot embedded static)${NC}"
else
    echo -e "${YELLOW}NOT DETECTED (Start via 'make dev' or 'make docker-up')${NC}"
fi

echo -e "${BLUE}========================================================================${NC}"
