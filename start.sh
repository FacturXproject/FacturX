#!/bin/sh
# One-command start: prepares the local configuration if missing, then builds and
# starts the whole stack. Usage: ./start.sh  (extra args go to docker compose up)

set -e

cd "$(dirname "$0")"

if [ ! -f .env ]; then
    cp .env.example .env
    echo ".env created from .env.example"
fi

if [ ! -f nginx/certs/localhost.crt ] || [ ! -f nginx/certs/localhost.key ]; then
    ./scripts/generate-certs.sh
fi

exec docker compose up --build "$@"
