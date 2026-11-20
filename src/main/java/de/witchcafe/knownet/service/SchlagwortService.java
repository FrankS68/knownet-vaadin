package de.witchcafe.knownet.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.repo.SchlagwortRepository;

/**
 * Schlagworte und ihre Hierarchie.
 * <p>
 * Die Hierarchie steckt in {@code (:Schlagwort)-[:UNTERBEGRIFF_VON]->(:Schlagwort)}.
 * Diese Kanten sind bewusst <b>nicht</b> in der Entity {@link Schlagwort} gemappt,
 * sondern werden hier per Cypher gepflegt: so kann ein Speichern von Quelle oder
 * Aussage (das die Schlagworte mitspeichert) die Hierarchie nie versehentlich loeschen.
 * Ein Schlagwort darf mehrere Oberbegriffe haben; Zyklen werden verhindert.
 */
@Service
public class SchlagwortService {

    /** Trenner fuer Pfade wie "weltwirtschaft/europawirtschaft/deutsche wirtschaft". */
    public static final String PFAD_TRENNER = "/";

    private final SchlagwortRepository repository;
    private final Neo4jClient client;

    public SchlagwortService(SchlagwortRepository repository, Neo4jClient client) {
        this.repository = repository;
        this.client = client;
    }

    /** Eintrag fuer Baumdarstellung: Schlagwort mit seinen direkten Oberbegriffen und Nutzungszahlen. */
    public record Eintrag(String name, List<String> oberbegriffe, long quellen, long aussagen) {}

    /**
     * Findet ein Schlagwort per Name oder legt es an.
     * Namen werden normalisiert (getrimmt, kleingeschrieben),
     * damit "Klima" und "klima " nicht zu zwei Knoten werden.
     */
    public Schlagwort findeOderErzeuge(String name) {
        String normalisiert = normalisiere(name);
        return repository.findByName(normalisiert)
                .orElseGet(() -> repository.save(new Schlagwort(normalisiert)));
    }

    /**
     * Wandelt eine kommaseparierte Eingabe in eine Menge von Schlagwort-Knoten um.
     * Jeder Eintrag darf ein Pfad sein ("weltwirtschaft/europawirtschaft/deutsche wirtschaft"):
     * dann werden alle Stufen angelegt, als Ober-/Unterbegriffe verknuepft und
     * nur das letzte (speziellste) Schlagwort zugeordnet.
     */
    public Set<Schlagwort> ausKommaListe(String eingabe) {
        Set<Schlagwort> ergebnis = new HashSet<>();
        if (eingabe == null || eingabe.isBlank()) {
            return ergebnis;
        }
        for (String teil : eingabe.split(",")) {
            if (!teil.isBlank()) {
                Schlagwort s = ausPfad(teil);
                if (s != null) {
                    ergebnis.add(s);
                }
            }
        }
        return ergebnis;
    }

    /** Legt alle Stufen eines Pfads an, verknuepft sie und liefert die letzte Stufe. */
    public Schlagwort ausPfad(String pfad) {
        Schlagwort vorher = null;
        for (String stufe : pfad.split(PFAD_TRENNER)) {
            if (stufe.isBlank()) {
                continue;
            }
            Schlagwort aktuell = findeOderErzeuge(stufe);
            if (vorher != null) {
                setzeOberbegriff(aktuell.getName(), vorher.getName());
            }
            vorher = aktuell;
        }
        return vorher;
    }

    /**
     * Ordnet {@code unterbegriff} dem {@code oberbegriff} unter.
     * Ohne Wirkung, wenn beide gleich sind oder ein Zyklus entstuende.
     *
     * @return true, wenn die Beziehung jetzt besteht
     */
    public boolean setzeOberbegriff(String unterbegriff, String oberbegriff) {
        String u = normalisiere(unterbegriff);
        String o = normalisiere(oberbegriff);
        if (u.equals(o)) {
            return false;
        }
        findeOderErzeuge(u);
        findeOderErzeuge(o);
        return client.query(
                "MATCH (u:Schlagwort {name: $u}), (o:Schlagwort {name: $o}) " +
                "WHERE NOT (o)-[:UNTERBEGRIFF_VON*0..]->(u) " +
                "MERGE (u)-[:UNTERBEGRIFF_VON]->(o) RETURN count(*) AS n")
                .bind(u).to("u").bind(o).to("o")
                .fetchAs(Long.class).one().orElse(0L) > 0;
    }

    /** Loest die direkte Ober-/Unterbegriff-Beziehung. */
    public void entferneOberbegriff(String unterbegriff, String oberbegriff) {
        client.query(
                "MATCH (:Schlagwort {name: $u})-[r:UNTERBEGRIFF_VON]->(:Schlagwort {name: $o}) DELETE r")
                .bind(normalisiere(unterbegriff)).to("u")
                .bind(normalisiere(oberbegriff)).to("o")
                .run();
    }

    /** Alle Schlagworte mit direkten Oberbegriffen und Nutzungszahlen (direkt zugeordnet). */
    public List<Eintrag> alleEintraege() {
        Collection<Map<String, Object>> zeilen = client.query(
                "MATCH (s:Schlagwort) " +
                "OPTIONAL MATCH (s)-[:UNTERBEGRIFF_VON]->(o:Schlagwort) " +
                "WITH s, collect(o.name) AS ober " +
                "RETURN s.name AS name, ober, " +
                "  COUNT { (:Quelle)-[:HAT_SCHLAGWORT]->(s) } AS quellen, " +
                "  COUNT { (:Aussage)-[:HAT_SCHLAGWORT]->(s) } AS aussagen " +
                "ORDER BY name")
                .fetch().all();
        List<Eintrag> ergebnis = new ArrayList<>();
        for (Map<String, Object> z : zeilen) {
            @SuppressWarnings("unchecked")
            List<String> ober = (List<String>) z.get("ober");
            ergebnis.add(new Eintrag((String) z.get("name"),
                    ober != null ? ober : List.of(),
                    ((Number) z.get("quellen")).longValue(),
                    ((Number) z.get("aussagen")).longValue()));
        }
        return ergebnis;
    }

    /** Sucht ein Schlagwort per (normalisiertem) Namen, ohne es anzulegen. */
    public Optional<Schlagwort> finde(String name) {
        return name == null || name.isBlank() ? Optional.empty() : repository.findByName(normalisiere(name));
    }

    /** Direkte Oberbegriffe, alphabetisch. */
    public List<String> oberbegriffe(String name) {
        return new ArrayList<>(client.query(
                "MATCH (:Schlagwort {name: $name})-[:UNTERBEGRIFF_VON]->(o:Schlagwort) RETURN o.name AS n ORDER BY n")
                .bind(normalisiere(name)).to("name")
                .fetchAs(String.class).all());
    }

    /** Direkte Unterbegriffe, alphabetisch. */
    public List<String> unterbegriffe(String name) {
        return new ArrayList<>(client.query(
                "MATCH (u:Schlagwort)-[:UNTERBEGRIFF_VON]->(:Schlagwort {name: $name}) RETURN u.name AS n ORDER BY n")
                .bind(normalisiere(name)).to("name")
                .fetchAs(String.class).all());
    }

    /** Namen aller Schlagworte, alphabetisch. */
    public List<String> alleNamen() {
        return new ArrayList<>(client.query("MATCH (s:Schlagwort) RETURN s.name AS name ORDER BY name")
                .fetchAs(String.class).all());
    }

    /**
     * IDs aller Knoten mit dem Label {@code label} ("Quelle" oder "Aussage"),
     * die das Schlagwort {@code name} selbst oder einen seiner (beliebig tiefen)
     * Unterbegriffe tragen.
     */
    public List<Long> idsMitSchlagwortOderUnterbegriff(String label, String name) {
        if (!"Quelle".equals(label) && !"Aussage".equals(label)) {
            throw new IllegalArgumentException("Unbekanntes Label: " + label);
        }
        return new ArrayList<>(client.query(
                "MATCH (n:" + label + ")-[:HAT_SCHLAGWORT]->(:Schlagwort)" +
                "-[:UNTERBEGRIFF_VON*0..]->(:Schlagwort {name: $name}) " +
                "RETURN DISTINCT id(n) AS id")
                .bind(normalisiere(name)).to("name")
                .fetchAs(Long.class).all());
    }

    private static String normalisiere(String name) {
        return name.trim().toLowerCase();
    }
}
