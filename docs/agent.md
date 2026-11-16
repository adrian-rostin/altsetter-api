# Agent-Leitfaden: Altsetter API

Kurz und vollständig: Was diese API ist, wie sie aufgebaut ist und welche Regeln bei Änderungen
gelten. Für Menschen stehen die Details im [README](../README.md), Entwurf und Endpunkte in
[`api.md`](api.md).

## Idee

Backend für [Altsetter](https://github.com/adrian-rostin/altsetter), einen Übungsfinder fürs Gym:
Man tippt einen Muskel an einem 3D-Körper an und bekommt passende Übungen. Die API liefert die
Übungen (Texte DE/EN, Equipment, Muskeln, Tipps, Fehler, Fotos), die Suche und ähnliche Übungen.

- Live: `https://altsetter.adrianrostin.de/api/v1`, gleiche Domain wie die App (kein CORS).
- **Nur lesen.** Alle Endpunkte sind `GET`. Die API hat keine Schreibrechte und ändert nie Daten.
- **Liefert nur, was die App braucht.** Muskeln und Muskelgruppen stehen fest in der App; die API
  nutzt sie aus der Datenbank nur intern (Namen, Reihenfolge, Übungen je Muskel).

## Wichtigste Regel: Datenbank

- Im Repository gibt es **nichts** zur Erstellung oder Änderung der Datenbank: keine Migrationen,
  kein Flyway/Liquibase, keine SQL-Skripte mit Daten. `ddl-auto: validate` prüft nur.
- Schema und Daten werden **nur direkt in der Datenbank im Cluster** gepflegt, per `kubectl exec`
  in `postgres-0` mit der Eigentümer-Rolle `altsetter`. Danach die API neu starten
  (`kubectl -n altsetter rollout restart deployment/altsetter-api`), weil sie alles im Speicher hält.
- Die API verbindet sich mit `altsetter_api`, die nur lesen darf.
- **Passwörter nie ausgeben**, nicht in Logs, Chat oder Dateien im Repository. Sie liegen im Secret
  `altsetter-db`; lokal schreibt `tools/local-env.sh` sie in `.env` (gitignored).
- Zerstörerische Aktionen im Cluster (Löschen, `DROP`, Neuanlegen) nur nach ausdrücklicher Zustimmung.

## Endpunkte

| Endpunkt                                 | Liefert                                                       |
| ---------------------------------------- | ------------------------------------------------------------- |
| `GET /api`, `GET /api/health`            | Version und Links, Status `UP`/`DOWN`                         |
| `GET /api/v1/exercises?page&size`        | Alle Übungen, seitenweise, nach Name (die App nutzt es nicht) |
| `GET /api/v1/exercises/search?q&page&size` | Suche, beste Treffer zuerst (Stufen, siehe `api.md`)        |
| `GET /api/v1/exercises/{id}`             | Alle Details einer Übung                                      |
| `GET /api/v1/exercises/{id}/similar`     | Bis zu fünf ähnliche Übungen (Kosinus über Muskeln, ≥ 0,5)    |
| `GET /api/v1/exercises/{id}/images/{start\|end}` | Foto als WebP, mit `?v=` unbegrenzt cachebar          |
| `GET /api/v1/muscles/{id}/exercises?page&size` | Übungen eines Muskels oder einer Gruppe, mit Details, in Kartenreihenfolge |

Gemeinsam für alle:

- Sprache über `Accept-Language` (`de` Standard, `en`); Antwort mit `Content-Language` und
  `Vary: Accept-Language`. Auch Fehlermeldungen sind übersetzt.
- Listen immer unter `items`; seitenweise mit `page`, `size`, `totalItems`, `totalPages`
  (`PageResponse.of(list, page, size)`), Seitengröße 1–100.
- IDs: `[a-z0-9]+(-[a-z0-9]+)*`, höchstens 64 Zeichen, englisch, in allen Sprachen gleich.
- Fehler als Problem Details (RFC 9457) mit festem `type` (`ProblemType`), Clients prüfen nur den `type`.
- `ETag` und `Cache-Control: max-age=300, public`; Fehler mit `no-store`.

Wer einen Endpunkt hinzufügt oder ändert, passt auch `README.md` und `docs/api.md` an, und in der
App `altsetter-api.ts` und `models/exercise.model.ts`.

## Struktur

```
src/main/java/de/adrianrostin/altsetter/
├── controller/   REST-Endpunkte; ApiPaths (Präfix, ID-Muster); Validierung per Bean Validation
├── service/      Logik: ExerciseService (Liste, Details, je Muskel), ExerciseSearchService,
│                 SimilarExerciseService, ExerciseImageService, MuscleService + MuscleCatalog
│                 (Muskelkatalog im Speicher), HealthService
├── repository/   Spring Data JPA, nur Lesemethoden
├── entity/       JPA-Entities, @Immutable; Enums mit festem Code (CodedEnum + Converter)
├── dto/          Antworten als Records; ListResponse, PageResponse
├── mapper/       Entity → DTO
├── exception/    ApiExceptionHandler, ProblemType, NotFoundException (Resource-Enum)
├── filter/       RequestIdFilter, RateLimitFilter (Bucket4j, pro IP), SecurityHeadersFilter
└── config/       WebConfig (Locale, ETags), CacheConfig (feste Liste aller Caches),
                  CacheHeadersAdvice, OpenApiConfig, RateLimitProperties
src/main/resources/
├── application.yaml, application-local.yaml
└── messages.properties (DE), messages_en.properties (EN)
src/test/java/…   Unit-Tests (*Test) und Integrationstests (*IT, Testcontainers + TestCatalog)
k8s/              Deployment, Service, Ingress, PostgreSQL-StatefulSet, NetworkPolicies (Kustomize)
k8s/setup/        Einmalig vom Admin: Namespace, Pipeline-Zugang, Traefik
tools/            db-tunnel.sh, local-env.sh, deployer-kubeconfig.sh
```

Schichten: Controller → Service → Repository. Nach außen gehen nur DTOs, nie Entities.

## Datenmodell (Schema `altsetter`)

`muscle_family` (+ `_text`), `muscle` (+ `muscle_text`, `muscle_mesh`), `equipment`
(+ `equipment_text`), `exercise` (+ `exercise_text`, `exercise_equipment`, `exercise_muscle` mit
Rolle `primary`/`secondary`, `exercise_tip` mit Art `cue`/`mistake`), `exercise_image` (WebP als
`bytea`, Position `start`/`end`; Halteübungen nur `start`). Texte je Sprache in den `*_text`-Tabellen.

## Caching

Daten ändern sich nur von Hand, deshalb Caffeine-Caches ohne Ablaufzeit (je Sprache): Katalog,
sortierte Übungsliste, Details, Übungen je Muskel, ähnliche Übungen, Bilder. Jeder neue Cache muss
in die feste Liste in `CacheConfig`. `@Cacheable` greift nur bei Aufrufen von außen, nicht innerhalb
derselben Klasse.

## Technik und Konventionen

- **Java 25, Spring Boot 4**, virtuelle Threads, Lombok, JPA/Hibernate, PostgreSQL 17.
- **Javadoc und Kommentare auf Deutsch**, Bezeichner und Testnamen auf Englisch.
- **Spotless** (Formatierung) und **Checkstyle** (u. a. Zeilen ≤ 120 Zeichen). Lange Annotationen
  umbrechen, Standardwerte als `String`-Konstanten (`PAGE_SIZE = "20"`).
- Alle Parameter begrenzen (`@Min`, `@Max`, `@Size`, `@Pattern`); neue Fehlermeldungen in beide
  `messages*.properties`.
- Tests: Logik als Unit-Test (statische Methoden wie `rank`, `tier`, `normalize`), Endpunkte als
  `*IT` gegen den kleinen `TestCatalog`. In `MockMvcTester` Leerzeichen und Sonderzeichen mit
  `.param(…)` übergeben, nicht in `uri(…)` (dort wird `%20` noch einmal kodiert).

## Befehle

```bash
export JAVA_HOME=…/openjdk-25            # JDK 25 nötig
tools/db-tunnel.sh                       # DB aus dem Cluster auf localhost:5432 (eigenes Terminal)
tools/local-env.sh                       # einmalig: Passwort der Leserolle nach .env
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # http://localhost:8080, Swagger: /api/docs
./mvnw spotless:apply                    # formatieren
./mvnw spotless:check checkstyle:check   # wie die Pipeline
./mvnw test                              # Unit-Tests
./mvnw verify                            # + Integrationstests (brauchen Docker)
```

Unter Windows `mvnw.cmd`. Lokal gibt es kein Docker: Integrationstests laufen nur in der CI. Vor dem
Push daher wenigstens `spotless:check checkstyle:check test-compile test`.

## CI/CD und Betrieb

`.github/workflows/ci-cd.yml`: Spotless und Checkstyle → `mvnw verify` (mit Testcontainers) →
Docker-Image mit Trivy-Scan nach `ghcr.io/adrian-rostin/altsetter-api` → Deploy mit
`kubectl apply -k k8s` (nur `main`). **Ein Push auf `main` geht direkt live.** Ein Deploy ändert
nichts an der Datenbank.

Betrieb im Namespace `altsetter`: zwei Replikas ohne Root mit schreibgeschütztem Dateisystem,
Health und Metriken auf Port 8081 (nicht über den Ingress), Logs als JSON mit Request-ID.

## Regeln für die Zusammenarbeit

- Nur committen und pushen, wenn ausdrücklich darum gebeten wird.
- Commit-Nachrichten auf Deutsch, kurz, ohne `Co-Authored-By` oder sonstige KI-Hinweise.
- Bei Änderungen, die das Frontend braucht: zuerst die API deployen, dann das Frontend.
- Datenschutz: keine Zugriffs-Logs, keine Speicherung von Suchbegriffen oder IPs (außer dem
  Rate-Limit-Zähler im Arbeitsspeicher, höchstens zehn Minuten). Das steht so in der
  Datenschutzerklärung im Portfolio; wer daran etwas ändert, muss den Text dort anpassen.
