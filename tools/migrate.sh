#!/usr/bin/env bash
# Spielt neue Migrationen über den Tunnel (tools/db-tunnel.sh) in die Datenbank im Cluster. Nur nötig
# bei der Einrichtung und wenn eine neue Migration dazukommt; die API selbst ändert nie etwas.
# Benutzer und Passwort der Eigentümer-Rolle kommen direkt aus dem Secret und werden nirgends gespeichert.
set -euo pipefail

cd "$(dirname "$0")/.."
secret() { kubectl -n altsetter get secret altsetter-db -o jsonpath="{.data.$1}" | base64 -d; }

SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/altsetter}" \
  SPRING_DATASOURCE_USERNAME="$(secret owner-username)" \
  SPRING_DATASOURCE_PASSWORD="$(secret owner-password)" \
  ./mvnw -B -ntp -q spring-boot:run -Dspring-boot.run.profiles=migrate -Dcheckstyle.skip
