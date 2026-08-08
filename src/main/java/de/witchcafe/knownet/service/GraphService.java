package de.witchcafe.knownet.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liefert Graph-Daten fuer die Neovis-Visualisierung.
 * Gibt Knoten und Kanten als einfache Maps zurueck,
 * die der REST-Controller als JSON serialisiert.
 */
@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private final Neo4jClient client;

    public GraphService(Neo4jClient client) {
        this.client = client;
    }

    public record GraphData(List<Map<String, Object>> nodes, List<Map<String, Object>> edges, String error) {}

    /**
     * Gibt alle Aussagen, Quellen und Schlagworte zurueck,
     * optional gefiltert nach einem Suchbegriff.
     * Falls Neo4j nicht erreichbar ist, wird ein Error-String zurueckgegeben.
     */
    public GraphData ladeGraph(String suche) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        try {
            boolean mitFilter = suche != null && !suche.isBlank();
            String filter = mitFilter ? suche.trim().toLowerCase() : null;

            // Aussagen laden
            Collection<Map<String, Object>> aussagen = client.query(
                "MATCH (a:Aussage) " +
                (mitFilter ? "WHERE toLower(a.text) CONTAINS $suche " : "") +
                "RETURN id(a) AS id, a.text AS text, 'Aussage' AS typ")
                .bind(filter).to("suche")
                .fetch().all();

        for (Map<String, Object> a : aussagen) {
            nodes.add(Map.of(
                    "id", "A_" + a.get("id"),
                    "label", kuerzen((String) a.get("text"), 60),
                    "title", a.get("text") != null ? a.get("text") : "",
                    "typ", "Aussage",
                    "color", "#7B61FF"
            ));
        }

        // Quellen laden
        Collection<Map<String, Object>> quellen = client.query(
                "MATCH (q:Quelle) " +
                (mitFilter ? "WHERE toLower(q.titel) CONTAINS $suche OR toLower(q.url) CONTAINS $suche " : "") +
                "RETURN id(q) AS id, q.titel AS titel, q.url AS url, 'Quelle' AS typ")
                .bind(filter).to("suche")
                .fetch().all();

        for (Map<String, Object> q : quellen) {
            String label = q.get("titel") != null && !((String)q.get("titel")).isBlank()
                    ? (String) q.get("titel") : (String) q.get("url");
            nodes.add(Map.of(
                    "id", "Q_" + q.get("id"),
                    "label", kuerzen(label, 40),
                    "title", label != null ? label : "",
                    "typ", "Quelle",
                    "color", "#00B4D8"
            ));
        }

        // Schlagworte laden
        Collection<Map<String, Object>> schlagworte = client.query(
                "MATCH (s:Schlagwort) " +
                (mitFilter ? "WHERE toLower(s.name) CONTAINS $suche " : "") +
                "RETURN id(s) AS id, s.name AS name, 'Schlagwort' AS typ")
                .bind(filter).to("suche")
                .fetch().all();

        for (Map<String, Object> s : schlagworte) {
            nodes.add(Map.of(
                    "id", "S_" + s.get("id"),
                    "label", s.get("name") != null ? (String) s.get("name") : "",
                    "title", s.get("name") != null ? (String) s.get("name") : "",
                    "typ", "Schlagwort",
                    "color", "#06D6A0"
            ));
        }

        // Kanten: Aussage -> Aussage (typisierte Beziehungen)
        Collection<Map<String, Object>> beziehungen = client.query(
                "MATCH (a:Aussage)-[r]->(b:Aussage) " +
                "RETURN id(r) AS id, id(a) AS von, id(b) AS zu, type(r) AS art, r.kommentar AS kommentar")
                .fetch().all();

        for (Map<String, Object> r : beziehungen) {
            edges.add(Map.of(
                    "id", "R_" + r.get("id"),
                    "from", "A_" + r.get("von"),
                    "to", "A_" + r.get("zu"),
                    "label", anzeigeName((String) r.get("art")),
                    "title", r.get("kommentar") != null ? (String) r.get("kommentar") : ""
            ));
        }

        // Kanten: Aussage -> Quelle (STAMMT_AUS)
        Collection<Map<String, Object>> stammtAus = client.query(
                "MATCH (a:Aussage)-[:STAMMT_AUS]->(q:Quelle) RETURN id(a) AS von, id(q) AS zu")
                .fetch().all();

        for (Map<String, Object> r : stammtAus) {
            edges.add(Map.of(
                    "id", "SA_" + r.get("von") + "_" + r.get("zu"),
                    "from", "A_" + r.get("von"),
                    "to", "Q_" + r.get("zu"),
                    "label", "stammt aus",
                    "title", ""
            ));
        }

        // Kanten: Aussage -> Schlagwort
        Collection<Map<String, Object>> aussageSchlagworte = client.query(
                "MATCH (a:Aussage)-[:HAT_SCHLAGWORT]->(s:Schlagwort) RETURN id(a) AS von, id(s) AS zu")
                .fetch().all();

        for (Map<String, Object> r : aussageSchlagworte) {
            edges.add(Map.of(
                    "id", "AS_" + r.get("von") + "_" + r.get("zu"),
                    "from", "A_" + r.get("von"),
                    "to", "S_" + r.get("zu"),
                    "label", "",
                    "title", ""
            ));
        }

        // Kanten: Quelle -> Schlagwort
        Collection<Map<String, Object>> quelleSchlagworte = client.query(
                "MATCH (q:Quelle)-[:HAT_SCHLAGWORT]->(s:Schlagwort) RETURN id(q) AS von, id(s) AS zu")
                .fetch().all();

        for (Map<String, Object> r : quelleSchlagworte) {
            edges.add(Map.of(
                    "id", "QS_" + r.get("von") + "_" + r.get("zu"),
                    "from", "Q_" + r.get("von"),
                    "to", "S_" + r.get("zu"),
                    "label", "",
                    "title", ""
            ));
        }

            return new GraphData(nodes, edges, null);
        } catch (Exception e) {
            log.error("Fehler beim Laden des Graphen von Neo4j", e);
            return new GraphData(new ArrayList<>(), new ArrayList<>(),
                    "Neo4j Datenbank nicht erreichbar: " + e.getMessage());
        }
    }

    private String kuerzen(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }

    private String anzeigeName(String typName) {
        try {
            return de.witchcafe.knownet.domain.BeziehungsArt.valueOf(typName).getAnzeigeName();
        } catch (Exception e) {
            return typName != null ? typName : "";
        }
    }
}
