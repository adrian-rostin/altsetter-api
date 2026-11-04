#!/usr/bin/env bash
# Legt .env für das Spring-Profil "local" an: Benutzer und Passwort der Leserolle aus dem Secret
# altsetter-db im Cluster. .env steht in .gitignore und landet nie im Repository.
set -euo pipefail

cd "$(dirname "$0")/.."
secret() { kubectl -n altsetter get secret altsetter-db -o jsonpath="{.data.$1}" | base64 -d; }
umask 077
{
  printf 'ALTSETTER_DB_USERNAME=%s\n' "$(secret api-username)"
  printf 'ALTSETTER_DB_PASSWORD=%s\n' "$(secret api-password)"
} > .env
echo ".env angelegt. Start: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"
