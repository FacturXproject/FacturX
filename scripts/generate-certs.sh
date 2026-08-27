#!/bin/sh

set -e

mkdir -p secrets

openssl req \
    -x509 \
    -nodes \
    -days 365 \
    -newkey rsa:2048 \
    -keyout secrets/localhost.key \
    -out secrets/localhost.crt \
    -subj "/CN=localhost"

echo "TLS certificate generated in secrets/"
