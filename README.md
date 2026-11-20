# Knownet

Wissensnetz: Quellen aus dem Internet erfassen und verschlagworten, Aussagen daraus extrahieren und untereinander typisiert verknüpfen (bestätigt, widerspricht, erweitert, ...). Speicherung in Neo4j.

## Starten

1. Neo4j starten (Docker Desktop muss laufen):

       docker compose up -d

   Neo4j Browser: http://localhost:7474 (Login: neo4j / knownet123)

2. Anwendung starten: in Eclipse `KnownetApplication` als Java Application ausführen
   (oder mit installiertem Maven: `mvn spring-boot:run`)

   UI: http://localhost:8082

## Datenmodell

- `(:Aussage)-[:STAMMT_AUS {zitat, fundstelle}]->(:Quelle)`
- `(:Quelle)-[:HAT_SCHLAGWORT]->(:Schlagwort)` und `(:Aussage)-[:HAT_SCHLAGWORT]->(:Schlagwort)`
- `(:Aussage)-[:BESTAETIGT|WIDERSPRICHT|ERWEITERT|...]->(:Aussage)` — Typen siehe `BeziehungsArt`-Enum
- `(:Schlagwort)-[:UNTERBEGRIFF_VON]->(:Schlagwort)` — Schlagwort-Hierarchie (mehrere Oberbegriffe erlaubt, Zyklen werden verhindert)

### Schlagwort-Hierarchie

Überall, wo Schlagworte kommasepariert eingegeben werden (UI, REST-API, JSON-Import), darf ein Eintrag ein Pfad sein:

    weltwirtschaft/europawirtschaft/deutsche wirtschaft/westerwald wirtschaft, energie

Fehlende Stufen werden angelegt und verknüpft; zugeordnet wird nur die letzte Stufe. Schlagwortnamen bleiben global eindeutig, d. h. später reicht `westerwald wirtschaft`. Die Ansicht „Schlagworte“ zeigt den Baum und erlaubt Autoren, Oberbegriffe zuzuordnen oder zu lösen. Der Schlagwort-Filter in Quellen und Aussagen findet auch alle Unterbegriffe.

Die `UNTERBEGRIFF_VON`-Kanten werden per Cypher im `SchlagwortService` gepflegt und sind absichtlich nicht in der Entity gemappt, damit das Speichern einer Quelle/Aussage sie nicht überschreibt.

### API-Doku für Agenten

Öffentlich, ohne API-Key:

- `GET /api/docs`: aktuelle API-Version und Links. Agenten prüfen das zu Beginn jedes Laufs.
- `GET /api/docs/anleitung`: Arbeitsanleitung (Markdown, `src/main/resources/api-doku/anleitung.md`).
- `GET /api/docs/aenderungen`: Änderungsprotokoll, neueste zuerst (`api/ApiAenderungen.java`).
- `GET /api/docs/openapi.json`: vollständige OpenAPI-Beschreibung, von springdoc aus den Controllern erzeugt.

**Bei jeder API-Änderung** oben in `ApiAenderungen.PROTOKOLL` einen Eintrag mit neuer Version ergänzen. Bei inkompatiblen Änderungen wird die MAJOR-Version erhöht und `inkompatibel = true` gesetzt. Falls nötig, auch die Anleitung anpassen. Neue Endpunkte bekommen `@Operation(summary = …)`.

### Recherchefragen an Agenten

`(:Frage {status, zustaendig, antwort, verlauf, ...})-[:ALS_AUSSAGE]->(:Aussage)`: Jede Frage steckt zugleich als Aussage im Netz. Agenten verknüpfen die Aussagen ihrer Quellen mit dieser Frage-Aussage (`BEANTWORTET`, `BESTAETIGT`, `WIDERSPRICHT`, `RELATIVIERT`, `BETRIFFT` …), dadurch steht die Frage im Kontext des vorhandenen Wissens. Status: `OFFEN` → `IN_ARBEIT` → `BEANTWORTET` → `ERLEDIGT`; Nachhaken setzt wieder auf `OFFEN`.

Ablauf für einen Agenten (API-Key als `Authorization: Bearer …`):

1. `GET /api/fragen/offen` – Fragen, die er übernehmen darf (`zustaendig` leer oder Anfang seines Key-Namens; in Arbeit hängengebliebene nach 24 h).
2. `POST /api/fragen/{id}/uebernehmen` – reserviert die Frage (409, wenn schon vergeben).
3. Quellen suchen und per `POST /api/import` anlegen; Verknüpfungen zur Frage mit `"zuId": <aussageId der Frage>`, vorhandene Quellen per `"quelleId"`. Das Ergebnis liefert die IDs je Ref (`quelleIds`, `aussageIds`).
4. `PUT /api/fragen/{id}/antwort` mit `{"antwort": "…"}` – oder `POST /api/fragen/{id}/freigeben` mit `{"grund": "…"}`.

`GET /api/fragen/{id}` zeigt auch `nachfrage`, `verlauf` und den bisherigen `kontext`. Agenten dürfen per `POST /api/fragen` selbst Fragen stellen.

Die typisierten Aussage-Beziehungen werden per Cypher über den `Neo4jClient` angelegt (`BeziehungService`), da Spring Data Neo4j keine dynamischen Relationship-Typen mappen kann. Neue Beziehungsarten: einfach im Enum ergänzen.

## Nützliche Cypher-Abfragen (Neo4j Browser)

Alles anzeigen:

    MATCH (n) OPTIONAL MATCH (n)-[r]->(m) RETURN n, r, m

Widersprüche finden:

    MATCH (a:Aussage)-[:WIDERSPRICHT]->(b:Aussage) RETURN a.text, b.text

Aussagen zu einem Schlagwort:

    MATCH (a:Aussage)-[:HAT_SCHLAGWORT]->(s:Schlagwort {name: "klima"}) RETURN a.text
