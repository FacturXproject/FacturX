#!/bin/sh

set -e

mkdir -p nginx/certs

openssl req \
    -x509 \
    -nodes \
    -days 365 \
    -newkey rsa:2048 \
    -keyout nginx/certs/localhost.key \
    -out nginx/certs/localhost.crt \
    -subj "/CN=localhost"

echo "TLS certificate generated in nginx/certs/"
