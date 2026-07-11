package de.witchcafe.knownet.service;

import java.util.Collection;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import de.witchcafe.knownet.domain.BeziehungsArt;

/**
 * Legt typisierte Beziehungen zwischen Aussagen direkt per Cypher an.
 * Spring Data Neo4j kann keine dynamischen Relationship-Typen mappen,
 * deshalb laeuft dieser Teil ueber den Neo4jClient. Der Typname stammt
 * ausschliesslich aus dem BeziehungsArt-Enum (Whitelist).
 */
@Service
public class BeziehungService {

    private final Neo4jClient client;

    public BeziehungService(Neo4jClient client) {
        this.client = client;
    }

    public void verknuepfe(Long vonAussageId, Long zuAussageId, BeziehungsArt art, String kommentar) {
        String cypher = String.format(
                "MATCH (a:Aussage) WHERE id(a) = $von "
              + "MATCH (b:Aussage) WHERE id(b) = $zu "
              + "MERGE (a)-[r:%s]->(b) "
              + "SET r.kommentar = $kommentar, r.erstelltAm = datetime()",
                art.name());

        client.query(cypher)
                .bind(vonAussageId).to("von")
                .bind(zuAussageId).to("zu")
                .bind(kommentar == null ? "" : kommentar).to("kommentar")
                .run();
    }

    public void aktualisiereKommentar(Long beziehungId, String kommentar) {
        client.query("MATCH (:Aussage)-[r]->(:Aussage) WHERE id(r) = $id SET r.kommentar = $kommentar")
                .bind(beziehungId).to("id")
                .bind(kommentar == null ? "" : kommentar).to("kommentar")
                .run();
    }

    public void loesche(Long beziehungId) {
        client.query("MATCH (:Aussage)-[r]->(:Aussage) WHERE id(r) = $id DELETE r")
                .bind(beziehungId).to("id")
                .run();
    }

    public Collection<Beziehung> alleBeziehungen() {
        return client.query(
                "MATCH (a:Aussage)-[r]->(b:Aussage) "
              + "RETURN id(r) AS id, a.text AS von, type(r) AS art, b.text AS zu, r.kommentar AS kommentar "
              + "ORDER BY r.erstelltAm DESC")
                .fetchAs(Beziehung.class)
                .mappedBy((typeSystem, record) -> new Beziehung(
                        record.get("id").asLong(),
                        record.get("von").asString(""),
                        anzeigeName(record.get("art").asString("")),
                        record.get("zu").asString(""),
                        record.get("kommentar").asString("")))
                .all();
    }

    public Map<String, Object> statistik() {
        return client.query(
                "MATCH (a:Aussage) WITH count(a) AS aussagen "
              + "MATCH (q:Quelle) WITH aussagen, count(q) AS quellen "
              + "OPTIONAL MATCH (:Aussage)-[r]->(:Aussage) "
              + "RETURN aussagen, quellen, count(r) AS beziehungen")
                .fetch()
                .one()
                .orElse(Map.of("aussagen", 0L, "quellen", 0L, "beziehungen", 0L));
    }

    private String anzeigeName(String typName) {
        try {
            return BeziehungsArt.valueOf(typName).getAnzeigeName();
        } catch (IllegalArgumentException e) {
            return typName;
        }
    }
}
