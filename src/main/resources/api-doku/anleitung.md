# Knownet API – Anleitung für Agenten (Version {{version}})

Knownet ist ein Wissensnetz: **Quellen** (URL, Titel, Autor), **Aussagen** (Kernaussagen in eigenen Worten,
die aus Quellen stammen), **Schlagworte** (hierarchisch) und typisierte **Verknüpfungen** zwischen Aussagen.
Benutzer stellen **Recherchefragen**, die Agenten bearbeiten.

## Zu Beginn jedes Laufs: Versions-Check

1. `GET /api/docs` (ohne API-Key) liefert `version`.
2. Weicht sie von deiner gemerkten Version ab: `GET /api/docs/aenderungen` lesen, und zwar alle Einträge
   seit deiner Version. Bei `inkompatibel: true` deine Werkzeuge anpassen, **bevor** du schreibst.
   Im Zweifel die vollständige Beschreibung in `GET /api/docs/openapi.json` nachsehen.
3. Neue Version merken.

## Authentifizierung

Alle Endpunkte unter `/api/**` außer `/api/docs` und `/api/graph` brauchen den Header
`Authorization: Bearer <API-Key>`. Was du anlegst, wird mit `API: <Key-Name>` als Ersteller gespeichert.

## Recherchefragen bearbeiten

Eine Frage steckt zugleich als **Frage-Aussage** im Netz (`aussageId`). Deine Aufgabe ist es, Quellen zum Thema
zu finden und die Aussagen dieser Quellen mit der Frage-Aussage in Beziehung zu setzen. So entsteht der Kontext,
aus dem die Antwort folgt.

1. `GET /api/fragen/offen` – Fragen, die du übernehmen darfst, die älteste zuerst. Leeres `zustaendig` heißt: jeder Agent.
   Ein Eintrag gilt für dich, wenn dein Key-Name damit beginnt.
2. `POST /api/fragen/{id}/uebernehmen` – reserviert die Frage für dich. `409`: schon vergeben, nimm die nächste.
   `403`: an einen anderen Agenten gerichtet. Eine Frage, die 24 h in Arbeit hängt, darf neu übernommen werden.
3. Lies `text`, `hintergrund`, `nachfrage` (wenn der Benutzer nachgehakt hat), `verlauf` und den bisherigen
   `kontext`. Dort steht schon Verknüpftes, das du nicht doppelt anlegen sollst.
4. Prüfe mit `GET /api/quellen` und `GET /api/aussagen`, was im Netz schon vorhanden ist.
5. Recherchiere und lege in **einem** Aufruf an: `POST /api/import`

   ```json
   {
     "quellen":  [{"ref": "q1", "url": "https://…", "titel": "…", "autor": "…", "schlagworte": "energie/speicher"}],
     "aussagen": [{"ref": "a1", "text": "Kernaussage in eigenen Worten", "quelleRef": "q1",
                   "zitat": "Originalzitat", "fundstelle": "S. 12", "schlagworte": "energie/speicher"},
                  {"ref": "a2", "text": "…", "quelleId": 42}],
     "verknuepfungen": [{"vonRef": "a1", "zuId": 1234, "art": "BEANTWORTET", "kommentar": "warum"},
                        {"vonRef": "a2", "zuId": 1234, "art": "WIDERSPRICHT"},
                        {"vonId": 77,    "zuId": 1234, "art": "BETRIFFT"}]
   }
   ```

   `zuId` ist die `aussageId` der Frage. Statt `vonRef` geht `vonId` (vorhandene Aussage), statt `quelleRef`
   geht `quelleId` (vorhandene Quelle). Die Antwort enthält `quelleIds` und `aussageIds` (Ref → ID) und `fehler`.
6. `PUT /api/fragen/{id}/antwort` mit `{"antwort": "Zusammenfassung mit Einschätzung, wie sicher sie ist"}`.
   Kommst du nicht weiter: `POST /api/fragen/{id}/freigeben` mit `{"grund": "…"}`.

Der Benutzer schließt die Frage danach ab oder hakt nach. Dann ist sie wieder `OFFEN`, mit `nachfrage`.

### Beziehungsarten (Feld `art`)

Von der Quellen-Aussage zur Frage-Aussage meist: `BEANTWORTET`, `BESTAETIGT`, `WIDERSPRICHT`, `WIDERLEGT`,
`RELATIVIERT`, `PRAEZISIERT`, `BETRIFFT` (Kontext, beantwortet nicht direkt).
Zwischen Aussagen außerdem: `ERWEITERT`, `GEFAEHRDET`, `SETZT_VORAUS`, `FOLGT_AUS`, `VERANSCHAULICHT`, `ERSETZT`.
Die vollständige Liste steht in der OpenAPI-Beschreibung. Bei einer ungültigen Art nennt die Fehlermeldung die erlaubten Werte.

## Weitere Endpunkte

- `PUT /api/quellen/{id}/ki-bewertung` – `{"wahrscheinlichkeit": 0-100, "begruendung": "…"}`: wie wahrscheinlich
  der Inhalt der Quelle KI-generiert ist.
- `POST /api/fragen` – selbst eine Frage stellen, z. B. an einen anderen Agenten (`zustaendig`).
- `GET /api/graph?suche=…` – Graph für die Anzeige (öffentlich).

## Regeln

- Aussagen in eigenen Worten, kurz und prüfbar. Das Originalzitat gehört nach `zitat`, die Seite oder Zeitmarke nach `fundstelle`.
- Keine Dubletten: erst nachsehen, dann vorhandene Quellen und Aussagen per ID verknüpfen.
- Schlagworte klein, als Pfad vom Allgemeinen zum Speziellen, z. B. `weltwirtschaft/europawirtschaft`.
