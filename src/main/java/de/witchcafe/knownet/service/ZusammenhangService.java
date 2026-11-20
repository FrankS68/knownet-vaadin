package de.witchcafe.knownet.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import de.witchcafe.knownet.domain.BeziehungsArt;

/**
 * Liefert die Zusammenhaenge fuer die Detailseiten:
 * welche Aussagen aus einer Quelle stammen und wie Aussagen untereinander verknuepft sind.
 */
@Service
public class ZusammenhangService {

    /**
     * Eine Beziehung aus Sicht einer bestimmten Aussage.
     *
     * @param ausgehend true: "diese Aussage &lt;art&gt; andere", false: "andere &lt;art&gt; diese Aussage"
     */
    public record AussageBeziehung(Long id, boolean ausgehend, String art,
                                   Long andereId, String andererText, String kommentar) {}

    private final Neo4jClient client;

    public ZusammenhangService(Neo4jClient client) {
        this.client = client;
    }

    /** IDs aller Aussagen, die aus der Quelle stammen. */
    public List<Long> aussageIdsZuQuelle(Long quelleId) {
        return new ArrayList<>(client.query(
                "MATCH (a:Aussage)-[:STAMMT_AUS]->(q:Quelle) WHERE id(q) = $id RETURN DISTINCT id(a) AS id")
                .bind(quelleId).to("id")
                .fetchAs(Long.class).all());
    }

    /**
     * Alle ein- und ausgehenden Beziehungen der angegebenen Aussagen,
     * gruppiert nach Aussage-ID (Aussagen ohne Beziehungen fehlen in der Map).
     */
    public Map<Long, List<AussageBeziehung>> beziehungenVon(Collection<Long> aussageIds) {
        Map<Long, List<AussageBeziehung>> ergebnis = new LinkedHashMap<>();
        if (aussageIds.isEmpty()) {
            return ergebnis;
        }
        client.query(
                "MATCH (a:Aussage)-[r]-(b:Aussage) WHERE id(a) IN $ids "
              + "RETURN id(a) AS selbst, id(r) AS id, startNode(r) = a AS ausgehend, type(r) AS art, "
              + "id(b) AS andere, b.text AS text, r.kommentar AS kommentar "
              + "ORDER BY r.erstelltAm")
                .bind(aussageIds).to("ids")
                .fetch().all()
                .forEach(z -> ergebnis
                        .computeIfAbsent(((Number) z.get("selbst")).longValue(), k -> new ArrayList<>())
                        .add(new AussageBeziehung(
                                ((Number) z.get("id")).longValue(),
                                Boolean.TRUE.equals(z.get("ausgehend")),
                                BeziehungsArt.anzeigeNameVon((String) z.get("art")),
                                ((Number) z.get("andere")).longValue(),
                                z.get("text") != null ? (String) z.get("text") : "",
                                z.get("kommentar") != null ? (String) z.get("kommentar") : "")));
        return ergebnis;
    }

    /** Beziehungen einer einzelnen Aussage. */
    public List<AussageBeziehung> beziehungenVon(Long aussageId) {
        return beziehungenVon(List.of(aussageId)).getOrDefault(aussageId, List.of());
    }
}
