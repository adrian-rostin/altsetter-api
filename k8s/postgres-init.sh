#!/bin/sh
# Läuft nur beim allerersten Start (leeres Datenverzeichnis) und richtet die Datenbank nach dem
# Prinzip der geringsten Rechte ein:
#
#   postgres       Superuser; nur lokal im Pod (kubectl exec), über das Netzwerk gesperrt
#   OWNER_USER     besitzt Datenbank und Schema "altsetter", sonst keine Sonderrechte;
#                  nur für Migrationen (tools/migrate.sh)
#   API_USER       darf sich verbinden und im Schema "altsetter" lesen, sonst nichts
#
# Namen und Passwörter kommen aus dem Secret altsetter-db und gehen als psql-Variablen hinein
# (:"name" für Bezeichner, :'name' für Texte); psql setzt sie sicher in Anführungszeichen.
# Tabellen und Daten spielt danach tools/migrate.sh ein.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v db="$POSTGRES_DB" \
  -v owner="$OWNER_USER" -v owner_password="$OWNER_PASSWORD" \
  -v api="$API_USER" -v api_password="$API_PASSWORD" <<-'SQL'
	-- Rollen
	CREATE ROLE :"owner" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'owner_password';
	CREATE ROLE :"api" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'api_password';
	ALTER ROLE :"owner" SET search_path = altsetter;
	ALTER ROLE :"api" SET search_path = altsetter;
	ALTER ROLE :"api" SET default_transaction_read_only = on;
	ALTER ROLE :"api" SET statement_timeout = '3s';

	-- Datenbank: gehört dem Eigentümer, sonst darf sich nur die API verbinden
	ALTER DATABASE :"db" OWNER TO :"owner";
	REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
	GRANT CONNECT ON DATABASE :"db" TO :"api";

	-- Eigenes Schema statt "public"; in "public" darf niemand etwas
	REVOKE ALL ON SCHEMA public FROM PUBLIC;
	CREATE SCHEMA altsetter AUTHORIZATION :"owner";
	GRANT USAGE ON SCHEMA altsetter TO :"api";
	ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA altsetter GRANT SELECT ON TABLES TO :"api";
SQL

# Der Superuser darf sich nicht über das Netzwerk anmelden. Die Regel steht vor allen anderen.
{ echo 'host all postgres all reject'; cat "$PGDATA/pg_hba.conf"; } > "$PGDATA/pg_hba.conf.new"
mv "$PGDATA/pg_hba.conf.new" "$PGDATA/pg_hba.conf"
