#!/usr/bin/env bash
# Tunnelt die Datenbank aus dem Cluster nach localhost:5432 und baut die Verbindung neu auf,
# wenn kubectl sie abbricht (passiert bei port-forward regelmäßig). Beenden mit Strg+C.
set -uo pipefail

NAMESPACE=altsetter
LOCAL_PORT="${1:-5432}"

trap 'exit 0' INT TERM
echo "Datenbank auf localhost:${LOCAL_PORT} (Strg+C beendet)"
while true; do
  kubectl -n "$NAMESPACE" port-forward svc/postgres "${LOCAL_PORT}:5432" >/dev/null
  echo "Verbindung verloren, neuer Versuch …"
  sleep 1
done
