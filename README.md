# Altsetter API

[![CI/CD](https://github.com/adrian-rostin/altsetter-api/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/adrian-rostin/altsetter-api/actions/workflows/ci-cd.yml)

REST-API für [Altsetter](https://altsetter.adrianrostin.de), den Übungsfinder fürs Gym. Liefert
Muskeln, Equipment und Übungen aus einer eigenen PostgreSQL-Datenbank. Die API ist rein lesend;
Swagger UI gibt es nur lokal unter `http://localhost:8080/api/docs` (siehe Lokal starten).

## Tech-Stack

| Bereich   | Technik                                                                               |
| --------- | ------------------------------------------------------------------------------------- |
| Sprache   | Java 25                                                                               |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Cache, Actuator), virtuelle Threads   |
| Datenbank | PostgreSQL 17, Hibernate 7, Flyway-Migrationen                                        |
| API       | REST, Problem Details (RFC 9457), ETags, OpenAPI/Swagger über springdoc               |
| Qualität  | Spotless (palantir-java-format), Checkstyle, JUnit 5, AssertJ, Testcontainers, JaCoCo |
| Betrieb   | Docker (Temurin JRE, ohne Root), Kubernetes (k3s), Traefik, GitHub Actions, GHCR, Prometheus-Metriken |

## Endpunkte

| Endpunkt                                  | Liefert                                                          |
| ----------------------------------------- | ---------------------------------------------------------------- |
| `GET /api`                                | Name und Version der API, Links zu Endpunkten, Doku und Health   |
| `GET /api/health`                         | `UP`, wenn API und Datenbank antworten, sonst `DOWN` mit `503`   |
| `GET /api/v1/muscle-families`             | Muskelgruppen mit ihren Anteilen                                 |
| `GET /api/v1/muscles`                     | Alle Muskeln mit ihren Meshes im 3D-Modell                       |
| `GET /api/v1/exercises`                   | Alle Übungen, seitenweise und nach Name sortiert                 |
| `GET /api/v1/exercises/{id}`              | Alle Angaben zu einer Übung                                      |

Die API liefert genau das, was die App braucht: Beim Start lädt sie Muskeln, Muskelgruppen und alle
Übungen, beim Öffnen einer Übung deren Details. IDs sind englisch, klein und mit Bindestrichen
(`upper-chest`, `lat-pulldown`) und in allen Sprachen gleich; dieselben IDs stehen in den Pfaden der
App. Listen haben ihre Einträge immer unter `items`. Mehr zum Entwurf in [`docs/api.md`](docs/api.md).

**Sprachen:** Alle Texte gibt es auf Deutsch und Englisch. Der Client wählt mit `Accept-Language`
(`en`, `en-US` …); ohne passende Angabe kommt Deutsch. Jede Antwort nennt die gelieferte Sprache in
`Content-Language` und hat `Vary: Accept-Language`, damit Caches die Sprachen getrennt halten. In
der Datenbank stehen Texte in eigenen Tabellen (`*_text`, `exercise_tip`) mit einer Zeile je
Sprache; eine weitere Sprache braucht nur einen Eintrag in `language` und ihre Texte.

## Projektstruktur

Klassischer Schichtenaufbau: Controller nehmen Anfragen an, Services enthalten die Logik,
Repositories den Datenbankzugriff. Nach außen gehen ausschließlich DTOs.

```
src/main/java/de/adrianrostin/altsetter/
├── controller/    REST-Endpunkte (Status, Muskeln, Übungen)
├── service/       Logik: Übungen, Health-Check, Muskelkatalog im Speicher
├── repository/    Spring-Data-JPA-Repositories, nur Lesemethoden
├── entity/        JPA-Entities, unveränderlich (@Immutable), Enums mit festem Code
├── dto/           Antworten der API als Records
├── mapper/        Entity → DTO
├── exception/     Fehler als Problem Details (RFC 9457) mit festen Typen
├── filter/        Request-ID, Rate-Limiting, Security-Header
└── config/        Web (ETags, Cache-Header), Caches, OpenAPI, Rate-Limit-Einstellungen

src/main/resources/db/migration/        Flyway: Schema und alle Daten (V1)
tools/db-tunnel.sh, local-env.sh        Lokale Entwicklung gegen die Datenbank im Cluster
tools/migrate.sh                        Migrationen über den Tunnel einspielen (nur bei neuen Migrationen)
tools/deployer-kubeconfig.sh            kubeconfig mit eingeschränkten Rechten für die Pipeline
k8s/                                    Deployment, PostgreSQL, Ingress, NetworkPolicies (Kustomize)
k8s/setup/                              Einmalig vom Admin: Namespace, Zugang der Pipeline, Traefik
```

## Schutz vor Überlast

- **Rate-Limiting** pro Besucher-IP (Token-Bucket mit Bucket4j): 300 Anfragen als Puffer, danach
  150 pro Minute. Ein Seitenaufruf der App braucht drei Anfragen, eine geöffnete Übung eine. Jede
  Antwort nennt den Stand in `RateLimit-Limit`, `RateLimit-Remaining` und `RateLimit-Reset`; darüber
  kommt `429` mit `Retry-After`. Hinter Cloudflare zählt die Besucher-IP aus `CF-Connecting-IP`, aber
  nur bei Anfragen aus den Cloudflare-Netzen. Einstellbar unter `altsetter.rate-limit`.
- **Timeouts:** Verbindungsaufbau 5 s (gegen langsam tröpfelnde Anfragen), höchstens 2 s Warten auf
  eine Datenbankverbindung, höchstens 3 s pro SQL-Abfrage (in Postgres erzwungen). Ist die Datenbank
  ausgelastet, kommt sofort `503` mit `Retry-After` statt einer hängenden Anfrage.
- **Begrenzte Antworten:** `GET /exercises` liefert höchstens 100 Einträge pro Seite. Alle
  Parameter haben Längen- und Wertgrenzen.
- **Caching:** Muskeln und Übungsdetails liegen nach dem ersten Abruf im Speicher. Browser
  dürfen Antworten fünf Minuten behalten und fragen danach per `ETag` nach (meist nur ein `304`).
  Fehler werden nie gecacht.

## Sicherheit

- **Keine Zugangsdaten im Repository.** Adresse, Benutzer und Passwörter der Datenbank liegen im
  Kubernetes-Secret `altsetter-db` und kommen als Umgebungsvariablen in die Pods
  (`SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD`). Lokal holen die Skripte sie aus demselben
  Secret, in eine `.env`, die nie eingecheckt wird.
- **Minimale Rechte in der Datenbank:** Die API verbindet sich mit einer Rolle, die nur lesen darf
  (zusätzlich `default_transaction_read_only` und 3 s Zeitlimit pro Abfrage). Schreiben kann nur der
  Eigentümer, und den nutzt ausschließlich `tools/migrate.sh`.
- **Eingaben:** Alle Parameter sind begrenzt und geprüft, IDs nur im Format `[a-z0-9-]` (auch per
  Constraint in der Datenbank). Alle Abfragen laufen über JPA und sind parametrisiert.
- **Header:** HSTS, eine strikte Content-Security-Policy (`default-src 'none'`), `nosniff`,
  `X-Frame-Options`, `Cross-Origin-Resource-Policy`. Fehler verraten keine Interna, nur eine
  Request-ID für das Log.
- **Netzwerk:** Die API ist nur über Traefik erreichbar, die Datenbank nur von der API
  (NetworkPolicies). Health und Metriken liegen auf einem eigenen Port ohne Ingress.
- **Container:** ohne Root, schreibgeschütztes Dateisystem, keine Linux-Capabilities,
  Seccomp-Profil, kein ServiceAccount-Token.
- **Lieferkette:** Actions und Basis-Images sind auf Commit bzw. Digest festgelegt, Dependabot hält
  sie aktuell, Trivy prüft jedes Image vor dem Push auf bekannte Lücken. Für die Pipeline gibt es
  einen Zugang, der nur im Namespace `altsetter` deployen und keine Secrets lesen darf.

## Fehler

Jeder Fehler ist ein Problem Detail (`application/problem+json`) mit festem `type`, Titel und
Meldung in der gewählten Sprache (`messages*.properties`) und der Request-ID, die auch im Header `X-Request-Id` und in jeder Logzeile steht:

```json
{
  "type": "/api/problems/not-found",
  "title": "Nicht gefunden",
  "status": 404,
  "detail": "Übung 'kreuzheben-im-handstand' gibt es nicht.",
  "instance": "/api/v1/exercises/kreuzheben-im-handstand",
  "requestId": "ff872bd7-6460-4bbe-9501-0faa2e920bbe"
}
```

| `type`                          | Status | Wann                                                        |
| ------------------------------- | -----: | ----------------------------------------------------------- |
| `/api/problems/invalid-parameter` |  400 | Wert passt nicht, z. B. `?size=500` (Details unter `errors`) |
| `/api/problems/not-found`       |    404 | Unbekannte ID oder unbekannter Pfad                         |
| `/api/problems/method-not-allowed` | 405 | Alles außer `GET`                                           |
| `/api/problems/rate-limited`    |    429 | Zu viele Anfragen                                           |
| `/api/problems/unavailable`     |    503 | Datenbank gerade ausgelastet                                |
| `/api/problems/internal-error`  |    500 | Unerwarteter Fehler (steht mit Request-ID im Log)           |

## Lokal starten

Voraussetzungen: JDK 25 und `kubectl` mit Zugriff auf den Cluster.

```bash
tools/db-tunnel.sh      # Datenbank aus dem Cluster auf localhost:5432 (eigenes Terminal)
tools/local-env.sh      # einmalig: Passwort der Leserolle nach .env
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Die API läuft dann auf `http://localhost:8080`, Swagger UI unter `http://localhost:8080/api/docs` (nur mit dem Profil `local`, in Produktion abgeschaltet).
Sie liest nur und spielt nie Migrationen ein. In IntelliJ reicht eine Spring-Boot-Run-Configuration
mit dem Profil `local`. Das Frontend (`npm start` im Projekt altsetter) leitet `/api` automatisch an
`localhost:8080` weiter.

| Befehl                                   | Zweck                                                             |
| ---------------------------------------- | ----------------------------------------------------------------- |
| `./mvnw spotless:apply`                  | Code formatieren                                                  |
| `./mvnw spotless:check checkstyle:check` | Formatierung und Code-Regeln prüfen (wie die Pipeline)            |
| `./mvnw test`                            | Unit-Tests                                                        |
| `./mvnw verify`                          | Dazu Integrationstests gegen Postgres in Docker und Testabdeckung |
| `tools/migrate.sh`                       | Neue Migrationen über den Tunnel einspielen                       |

## Daten

| Inhalt        | Anzahl | Herkunft                                                                 |
| ------------- | -----: | ------------------------------------------------------------------------ |
| Muskelgruppen |      9 | Altsetter-App (z. B. Schulter mit vorderem, seitlichem, hinterem Anteil) |
| Muskeln       |     38 | Altsetter-App, inkl. Mesh-Namen im 3D-Modell                             |
| Equipment     |     23 | eigene Liste                                                             |
| Übungen       |    142 | eigene Texte und Muskelzuordnung                                         |

Die Daten liegen dauerhaft in der Datenbank im Cluster; die API liest sie nur. Angelegt hat sie
einmalig die Migration `V1__init.sql` (Schema und alle Daten), eingespielt mit `tools/migrate.sh`.
Ein Deploy ändert an den Daten nichts.

**Daten ändern:** eine neue Migration (`V2__…sql`) anlegen und mit `tools/migrate.sh` einspielen.
Die Integrationstests laufen gegen genau diese Migrationen, ein Fehler fällt also schon in der
Pipeline auf.

## CI/CD

Jeder Push und jeder Pull Request durchläuft [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml):

1. **Format & Lint:** Spotless und Checkstyle
2. **Build & Test:** Unit- und Integrationstests (Testcontainers), Testberichte als Artefakt
3. **Docker image:** Multi-Stage-Build mit Spring-Boot-Schichten, Sicherheitsscan mit Trivy, auf
   `main` Push nach `ghcr.io/adrian-rostin/altsetter-api`
4. **Deploy** (nur `main`): `kubectl apply -k k8s` mit dem Commit-Tag des Images, danach wird auf
   den erfolgreichen Rollout gewartet

Für das Deployment braucht das Repository das Secret `KUBECONFIG` (siehe Ersteinrichtung).

## Betrieb

Alles läuft im Namespace `altsetter`, der in Rancher zum Projekt „adrian“ gehört:

- **API:** zwei Replikas, ohne Root, schreibgeschütztes Dateisystem. Health-Checks und
  Prometheus-Metriken (`/actuator/prometheus`) auf Port 8081, nicht über den Ingress erreichbar.
  Bereit (`/actuator/health/readiness`) ist ein Pod nur mit Verbindung zur Datenbank. Logs als
  JSON (ECS) mit Request-ID.
- **Routing:** unter `altsetter.adrianrostin.de/api`; alles andere geht an das Frontend
- **PostgreSQL 17:** als StatefulSet, nur von den API-Pods erreichbar (NetworkPolicy). Prüfsummen
  auf Datenblöcken, alle Tabellen im eigenen Schema `altsetter`. Das Volume bleibt erhalten, auch
  wenn StatefulSet oder Claim gelöscht werden (`Retain`).

| Rolle           | Rechte                                   | Genutzt von                         |
| --------------- | ---------------------------------------- | ----------------------------------- |
| `postgres`      | Superuser, nur lokal im Pod              | Wartung per `kubectl exec`          |
| `altsetter`     | besitzt Datenbank und Schema, sonst nichts | nur `tools/migrate.sh`            |
| `altsetter_api` | darf nur lesen (Rechte und read-only)    | die API, lokale Entwicklung         |

Alle Zugangsdaten liegen im Secret `altsetter-db` und nicht im Repository. Die Rollen legt
`k8s/postgres-init.sh` an; Postgres führt es nur beim allerersten Start mit leerem Volume aus,
danach nie wieder.

**Ersteinrichtung** (einmalig, auf einem neuen Server):

```bash
kubectl apply -f k8s/setup/namespace.yaml
kubectl -n altsetter create secret generic altsetter-db \
  --from-literal=url=jdbc:postgresql://postgres:5432/altsetter \
  --from-literal=admin-password="$(openssl rand -hex 24)" \
  --from-literal=owner-username=altsetter \
  --from-literal=owner-password="$(openssl rand -hex 24)" \
  --from-literal=api-username=altsetter_api \
  --from-literal=api-password="$(openssl rand -hex 24)"
kubectl apply -k k8s          # Postgres startet und legt Rollen und Schema an
tools/migrate.sh              # Tabellen und Daten (V1) einspielen, über tools/db-tunnel.sh
```

Optional, aber empfohlen:

- `kubectl apply -f k8s/setup/traefik-client-ip.yaml`: Traefik sieht die echten Besucher-IPs. Ohne
  das ersetzt k3s sie durch eine interne Adresse und alle Besucher teilen sich ein Rate-Limit.
- `kubectl apply -f k8s/setup/deployer.yaml` und
  `tools/deployer-kubeconfig.sh https://<api-server>:6443 | gh secret set KUBECONFIG`: Die Pipeline
  darf dann nur im Namespace `altsetter` deployen und keine Secrets lesen.

Die Datenbank bleibt, wie sie ist: Deploys und Neustarts ändern nichts an ihr. Sollte sie doch
einmal verloren gehen, lässt sie sich mit der Ersteinrichtung aus `V1__init.sql` vollständig neu
aufbauen.

## Lizenz

Quellcode, Muskelkatalog und Übungstexte sind eigene Inhalte.
