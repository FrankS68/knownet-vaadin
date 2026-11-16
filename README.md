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

Die typisierten Aussage-Beziehungen werden per Cypher über den `Neo4jClient` angelegt (`BeziehungService`), da Spring Data Neo4j keine dynamischen Relationship-Typen mappen kann. Neue Beziehungsarten: einfach im Enum ergänzen.

## Nützliche Cypher-Abfragen (Neo4j Browser)

Alles anzeigen:

    MATCH (n) OPTIONAL MATCH (n)-[r]->(m) RETURN n, r, m

Widersprüche finden:

    MATCH (a:Aussage)-[:WIDERSPRICHT]->(b:Aussage) RETURN a.text, b.text

Aussagen zu einem Schlagwort:

    MATCH (a:Aussage)-[:HAT_SCHLAGWORT]->(s:Schlagwort {name: "klima"}) RETURN a.text
