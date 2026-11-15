# API-Konzept

Aufbau und Entscheidungen hinter der API. Die lebende Dokumentation liefert Swagger UI unter
`/api/docs`, aber nur lokal (Profil `local`); in Produktion ist sie abgeschaltet.

## Grundsätze

- **Basis-URL:** `https://altsetter.adrianrostin.de/api/v1`. Gleiche Domain wie die App, deshalb
  kein CORS nötig. Die Version im Pfad erlaubt später brechende Änderungen, ohne die App zu brechen.
- **Nur lesen:** Alle Endpunkte sind `GET`. Die Daten liegen dauerhaft in der Datenbank und werden
  nur von Hand dort geändert; eine Admin-Oberfläche kann später dazukommen.
- **Einstieg:** `GET /api` nennt Version und Links, `GET /api/health` meldet `UP` oder `DOWN` (503).
- **Nur was die App braucht:** Tippt man einen Muskel an, kommt die erste Seite seiner Übungen mit
  allen Details, beim Scrollen die nächste; Suche, ähnliche Übungen und Links laden
  einzelne. Muskeln und Muskelgruppen ändern sich nicht und stehen fest in der App; die API nutzt
  sie aus der Datenbank nur intern (Namen und Reihenfolge der Muskeln in den Übungen) und liefert sie
  nicht mehr einzeln aus.
- **Ein ID-System, englisch und für alle Sprachen gleich:** Muskelgruppen wie `shoulders`, Muskeln wie
  `side-delts`, Übungen wie `lat-pulldown`, Equipment wie `dumbbell`. Format: `[a-z0-9]+(-[a-z0-9]+)*`,
  höchstens 64 Zeichen. Die App benutzt dieselben IDs in ihren Pfaden
  (`/muscle/shoulders/dumbbell-lateral-raises`).
- **Texte auf Deutsch oder Englisch**, gewählt mit `Accept-Language` (Standard: Deutsch). Die Antwort
  nennt die Sprache in `Content-Language` und hat `Vary: Accept-Language`. Auch Fehlermeldungen
  kommen in der gewählten Sprache. Feldnamen sind englisch (camelCase).
- **Listen** haben ihre Einträge immer unter `items`; so kann eine Antwort später Metadaten bekommen,
  ohne Clients zu brechen. Seitenweise Listen haben dazu `page`, `size`, `totalItems`, `totalPages`.
- **Fehler** als Problem Details nach RFC 9457 mit festem `type` (siehe README). Clients unterscheiden
  Fehler am `type`, nie am Text.
- **Caching:** `ETag` und `Cache-Control: max-age=300, public`; Fehler mit `no-store`. In der API
  liegen Katalog, sortierte Übungsliste und Details je Sprache im Speicher; nach einer Änderung der Daten
  wird die API neu gestartet.

## Übungen

| Endpunkt                           | Liefert                                                 |
| ---------------------------------- | ------------------------------------------------------- |
| `GET /exercises`                   | Alle Übungen, seitenweise, nach Name in der Sprache     |
| `GET /exercises/search?q=…`        | Suche, die besten Treffer zuerst (siehe Suche)          |
| `GET /exercises/{id}`              | Alle Details einer Übung                                |
| `GET /exercises/{id}/similar`      | Bis zu fünf ähnliche Übungen (siehe Ähnliche Übungen)   |
| `GET /exercises/{id}/images/{pos}` | Foto in Start- oder Endstellung (siehe Bilder)          |
| `GET /muscles/{id}/exercises`      | Alle Übungen eines Muskels oder einer Gruppe, mit Details |

`GET /exercises` nimmt `page` (ab 0) und `size` (1–100, Standard 20). Die App braucht die ganze Liste
nicht; sie lädt Übungen je Muskel (siehe unten).

```json
// ExerciseSummary – Listeneintrag
{
  "id": "incline-barbell-bench-press",
  "name": "Schrägbankdrücken mit der Langhantel",
  "description": "Bank auf etwa 30° stellen …",
  "difficulty": "intermediate",
  "equipment": [{ "id": "barbell", "name": "Langhantel" }, { "id": "incline-bench", "name": "Schrägbank" }],
  "primaryMuscles": ["upper-chest"],
  "secondaryMuscles": ["mid-chest", "front-delts", "triceps-lateral-head"],
  "images": [
    { "position": "start", "url": "/api/v1/exercises/incline-barbell-bench-press/images/start?v=3f9c…", "width": 1536, "height": 1024 },
    { "position": "end", "url": "/api/v1/exercises/incline-barbell-bench-press/images/end?v=a41d…", "width": 1536, "height": 1024 }
  ]
}
```

```json
// GET /exercises/incline-barbell-bench-press   (Accept-Language: en)
{
  "id": "incline-barbell-bench-press",
  "name": "Incline Barbell Bench Press",
  "description": "…",
  "difficulty": "intermediate",
  "equipment": [ … ],
  "muscles": {
    "primary": [{ "id": "upper-chest", "name": "Upper Chest" }],
    "secondary": [{ "id": "mid-chest", "name": "Mid Chest" }, …]
  },
  "cues": ["Bench at 30–45° …", …],
  "mistakes": ["Hips lifting off the bench …", …],
  "images": [ … ]
}
```

### Übungen je Muskel

`GET /muscles/{id}/exercises?page=0&size=20` liefert alle Übungen, die einen Muskel als Haupt- oder
mitarbeitenden Muskel trainieren, jeweils mit allen Angaben wie `GET /exercises/{id}`, seitenweise
wie `GET /exercises`. Die ID ist ein Muskel (`side-delts`) oder eine Muskelgruppe (`shoulders`,
dann zählen alle ihre Anteile). Unbekannte IDs geben `404`.

Die Reihenfolge ist die der Übungskarte in der App: je Anteil (von oben nach unten am Körper) seine
Hauptübungen, danach alle, bei denen ein Anteil nur mitarbeitet; jeweils nach Name, jede Übung
einmal. So hängt die App die Seiten einfach aneinander. Sie lädt die erste Seite, wenn man einen
Muskel antippt, und die nächsten beim Scrollen; die Übungsseiten öffnen sich danach ohne weiteren
Abruf.

### Suche

`GET /exercises/search?q=…&page=0&size=20` liefert alle Übungen (wie in der Liste) zur Eingabe im
Suchfeld der App, seitenweise wie `GET /exercises` (`page` ab 0, `size` 1–100, Standard 20; dazu
`totalItems`, `totalPages`). Die App lädt beim Scrollen die nächste Seite nach. Leere Eingaben geben
`400`. Groß-/Kleinschreibung,
Umlaute, Akzente und Satzzeichen zählen nicht. Die Treffer kommen in Stufen, innerhalb einer Stufe
nach Name:

1. Der Name beginnt mit der Eingabe ("Landm" → Landmine Press, schon ab "L" weit oben).
2. Jedes Wort der Eingabe beginnt ein Wort im Namen ("curls lang" → Langhantel-Curls).
3. Jedes Wort der Eingabe steht irgendwo im Namen ("drücken" → Schulterdrücken).
4. Jedes Wort beginnt ein Wort in Name, Hauptmuskeln oder Equipment ("bizeps", "kurzhantel").
5. Der Name in einer anderen Sprache passt ("bench press" bei deutscher Oberfläche).

### Ähnliche Übungen

`GET /exercises/{id}/similar` liefert bis zu fünf Übungen (wie in der Liste), die ähnlichste zuerst:

- Jede Übung ist ein Vektor über die Muskeln, Hauptmuskeln zählen 2, mitarbeitende 1. Ähnlichkeit ist
  der Kosinus zweier Vektoren (0 bis 1), damit Übungen mit vielen Muskeln nicht allein deshalb vorn
  landen. Gleiche Muskeln in gleichen Rollen ergeben 1, z. B. Schrägbank-Flys zu Cable Flys auf der
  Schrägbank.
- Infrage kommt eine Übung nur, wenn sie einen Hauptmuskel der Übung trainiert und die Übung
  umgekehrt einen ihrer Hauptmuskeln. So zählen vertauschte Rollen (Hammer-Curls und Hammer-Curls am
  Seilzug), aber nicht Übungen, die den Muskel nur nebenbei treffen.
- Unter 0,5 passt es nicht mehr. Dann sind es lieber weniger als fünf oder keine.

## Bilder

Jede Übung hat ein Foto der Start- und eines der Endstellung (WebP, 1536 × 1024, rund 55 KB).
Halteübungen wie Unterarm- und Seitstütz haben nur das Startbild, das die gehaltene Position zeigt.
Die Fotos liegen in der Tabelle `exercise_image` (Spalte `data`, `bytea`) und sind mit KI erstellt.

- **Liste und Details** nennen die Fotos in `images`, Start vor Ende. Die App zeigt das Startbild in
  der Liste und auf der Detailseite Start- und Endbild im Wechsel.
- **`GET /exercises/{id}/images/{start|end}`** liefert die Datei. Fehlt das Bild, kommt `404`, eine
  andere Stellung als `start`/`end` gibt `400`.
- **Caching:** Die URL in `images` enthält `v`, die ersten zwölf Zeichen des MD5 der Datei. Passt `v`,
  antwortet die API mit `Cache-Control: max-age=31536000, public, immutable`; ein neues Bild bekommt
  eine neue URL. Ohne `v` darf ein Bild einen Tag im Cache bleiben. In der API liegen Liste und
  Dateien im Speicher.
