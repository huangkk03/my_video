#!/bin/bash

set -e

echo "=== MKV Video Platform Deployment Script ==="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check prerequisites
command -v docker >/dev/null 2>&1 || { echo -e "${RED}Docker is required but not installed.${NC}" >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo -e "${RED}Docker Compose is required but not installed.${NC}" >&2; exit 1; }

# Parse arguments
FORCE_BUILD=false
SKIP_DB=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --force-build|-f)
      FORCE_BUILD=true
      shift
      ;;
    --skip-db|-s)
      SKIP_DB=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  --force-build, -f    Force rebuild without cache"
      echo "  --skip-db, -s        Skip database initialization"
      echo "  --help, -h           Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Check if .env exists
if [ ! -f .env ]; then
  echo -e "${YELLOW}Warning: .env file not found. Using default values.${NC}"
  cp .env.example .env 2>/dev/null || true
fi

# Build and deploy
deploy() {
  echo -e "${GREEN}Stopping existing containers...${NC}"
  docker-compose down -v
  
  if [ "$FORCE_BUILD" = true ]; then
    echo -e "${GREEN}Building images (no cache)...${NC}"
    docker-compose build --no-cache
  else
    echo -e "${GREEN}Building images...${NC}"
    docker-compose build
  fi
  
  echo -e "${GREEN}Starting services...${NC}"
  docker-compose up -d
  
  echo -e "${GREEN}Waiting for services to be healthy...${NC}"
  sleep 10
  
  # Check health
  for i in {1..30}; do
    if docker-compose ps | grep -q "healthy"; then
      echo -e "${GREEN}Services are healthy!${NC}"
      break
    fi
    echo -n "."
    sleep 2
  done
  echo ""
  
  # Show status
  echo -e "${GREEN}=== Deployment Complete ===${NC}"
  docker-compose ps
  
  echo -e "${YELLOW}Access the application:${NC}"
  echo "  - UI: http://localhost"
  echo "  - API: http://localhost:8080"
}

# Quick restart
restart() {
  echo -e "${GREEN}Restarting services...${NC}"
  docker-compose restart
  docker-compose ps
}

# Stop and clean
clean() {
  echo -e "${YELLOW}Stopping and removing all containers and volumes...${NC}"
  docker-compose down -v
  echo -e "${GREEN}Clean complete!${NC}"
}

# Show logs
logs() {
  docker-compose logs -f "$@"
}

# Main menu
case "${1:-deploy}" in
  deploy)
    deploy
    ;;
  restart)
    restart
    ;;
  clean)
    clean
    ;;
  logs)
    logs "${@:2}"
    ;;
  *)
    echo "Usage: $0 {deploy|restart|clean|logs}"
    exit 1
    ;;
esac
