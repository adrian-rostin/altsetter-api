# API-Konzept

Aufbau und Entscheidungen hinter der API. Die lebende Dokumentation liefert Swagger UI unter
`/api/docs`, aber nur lokal (Profil `local`); in Produktion ist sie abgeschaltet.

## Grundsätze

- **Basis-URL:** `https://altsetter.adrianrostin.de/api/v1`. Gleiche Domain wie die App, deshalb
  kein CORS nötig. Die Version im Pfad erlaubt später brechende Änderungen, ohne die App zu brechen.
- **Nur lesen:** Alle Endpunkte sind `GET`. Die Daten liegen dauerhaft in der Datenbank und werden
  nur von Hand dort geändert; eine Admin-Oberfläche kann später dazukommen.
- **Einstieg:** `GET /api` nennt Version und Links, `GET /api/health` meldet `UP` oder `DOWN` (503).
- **Nur was die App braucht:** Muskeln, Muskelgruppen und alle Übungen beim Start, Details beim
  Öffnen einer Übung. Gefiltert wird in der App, weil sie ohnehin alles kennt.
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

## Muskeln

| Endpunkt                     | Liefert                                                        |
| ---------------------------- | -------------------------------------------------------------- |
| `GET /muscle-families`       | Alle Gruppen (z. B. Schulter) mit den IDs ihrer Anteile        |
| `GET /muscles`               | Alle Muskeln mit Gruppe, Region, Beschreibung und Mesh-Namen   |

Die App lädt beide Listen einmal beim Start und weiß damit, welches Mesh im 3D-Modell zu welchem
Muskel gehört. Die Reihenfolge ist fest: von oben nach unten am Körper.

```json
// Ein Eintrag aus GET /muscles
{
  "id": "side-delts",
  "familyId": "shoulders",
  "name": "Seitliche Schulter",
  "latinName": "Deltoideus – Pars acromialis",
  "region": "Schultern",
  "summary": "…",
  "meshNames": ["Acromial part of deltoid muscle"]
}
```

## Übungen

| Endpunkt                           | Liefert                                                 |
| ---------------------------------- | ------------------------------------------------------- |
| `GET /exercises`                   | Alle Übungen, seitenweise, nach Name in der Sprache     |
| `GET /exercises/{id}`              | Alle Details einer Übung                                |
| `GET /exercises/{id}/images/{pos}` | Foto in Start- oder Endstellung (siehe Bilder)          |

`GET /exercises` nimmt `page` (ab 0) und `size` (1–100, Standard 20). Die App holt alle 142 Übungen
mit `size=100` in zwei Abrufen.

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
