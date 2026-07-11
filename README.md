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

Die typisierten Aussage-Beziehungen werden per Cypher über den `Neo4jClient` angelegt (`BeziehungService`), da Spring Data Neo4j keine dynamischen Relationship-Typen mappen kann. Neue Beziehungsarten: einfach im Enum ergänzen.

## Nützliche Cypher-Abfragen (Neo4j Browser)

Alles anzeigen:

    MATCH (n) OPTIONAL MATCH (n)-[r]->(m) RETURN n, r, m

Widersprüche finden:

    MATCH (a:Aussage)-[:WIDERSPRICHT]->(b:Aussage) RETURN a.text, b.text

Aussagen zu einem Schlagwort:

    MATCH (a:Aussage)-[:HAT_SCHLAGWORT]->(s:Schlagwort {name: "klima"}) RETURN a.text
