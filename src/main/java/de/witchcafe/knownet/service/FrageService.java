package de.witchcafe.knownet.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Frage;
import de.witchcafe.knownet.domain.FrageStatus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.FrageRepository;

/**
 * Ablauf der Recherchefragen.
 * <ol>
 *   <li>Benutzer stellt eine Frage → Status OFFEN, die Frage wird zugleich als Aussage angelegt.</li>
 *   <li>Ein Agent uebernimmt sie → IN_ARBEIT (atomar, damit nie zwei Agenten dieselbe Frage bearbeiten).
 *       Bleibt sie laenger als {@link #UEBERNAHME_VERFALLS_STUNDEN} Stunden haengen, darf ein anderer sie uebernehmen.</li>
 *   <li>Der Agent legt Quellen/Aussagen an, verknuepft sie mit der Frage-Aussage und antwortet → BEANTWORTET.</li>
 *   <li>Der Benutzer schliesst ab (ERLEDIGT) oder hakt nach (wieder OFFEN, mit Nachfrage).</li>
 * </ol>
 */
@Service
public class FrageService {

    public static final int UEBERNAHME_VERFALLS_STUNDEN = 24;
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final FrageRepository frageRepository;
    private final AussageRepository aussageRepository;
    private final SchlagwortService schlagwortService;
    private final Neo4jClient client;

    public FrageService(FrageRepository frageRepository, AussageRepository aussageRepository,
                        SchlagwortService schlagwortService, Neo4jClient client) {
        this.frageRepository = frageRepository;
        this.aussageRepository = aussageRepository;
        this.schlagwortService = schlagwortService;
        this.client = client;
    }

    /** Legt Frage und zugehoerige Frage-Aussage an. */
    public Frage stellen(String text, String hintergrund, String schlagworte, String zustaendig, String wer) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Fragetext fehlt");
        }
        Aussage aussage = new Aussage(text.trim());
        aussage.setSchlagworte(schlagwortService.ausKommaListe(schlagworte));
        Frage frage = new Frage(text.trim());
        frage.setHintergrund(leerZuNull(hintergrund));
        frage.setZustaendig(leerZuNull(zustaendig));
        frage.setGestelltVon(wer);
        frage.setAussage(aussageRepository.save(aussage));
        return frageRepository.save(frage);
    }

    public Optional<Frage> finde(Long id) {
        return frageRepository.findById(id);
    }

    /** Alle Fragen, neueste zuerst; optional nur ein Status. */
    public List<Frage> alle(FrageStatus status) {
        return frageRepository.findAll().stream()
                .filter(f -> status == null || f.getStatus() == status)
                .sorted(Comparator.comparing(Frage::getGestelltAm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();
    }

    /** Offene (oder haengengebliebene) Fragen, die der Agent {@code agent} uebernehmen darf, aelteste zuerst. */
    public List<Frage> uebernehmbarFuer(String agent) {
        LocalDateTime grenze = LocalDateTime.now().minusHours(UEBERNAHME_VERFALLS_STUNDEN);
        return frageRepository.findAll().stream()
                .filter(f -> f.getStatus() == FrageStatus.OFFEN
                        || (f.getStatus() == FrageStatus.IN_ARBEIT && f.getInArbeitSeit() != null
                            && f.getInArbeitSeit().isBefore(grenze)))
                .filter(f -> istZustaendig(f, agent))
                .sorted(Comparator.comparing(Frage::getGestelltAm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .toList();
    }

    /** true, wenn kein Agent eingetragen ist oder der Name des Agenten mit dem Eintrag beginnt. */
    public static boolean istZustaendig(Frage f, String agent) {
        if (f.getZustaendig() == null) {
            return true;
        }
        return agent != null && agent.toLowerCase().startsWith(f.getZustaendig().toLowerCase());
    }

    /**
     * Uebernimmt die Frage atomar fuer {@code wer}.
     *
     * @return true, wenn die Uebernahme geklappt hat; false, wenn sie schon bearbeitet wird oder nicht offen ist
     */
    public boolean uebernehmen(Long id, String wer) {
        // Zeiten aus der JVM, damit sie zu den von Java gesetzten LocalDateTime-Werten passen
        LocalDateTime jetzt = LocalDateTime.now();
        long n = client.query(
                "MATCH (f:Frage) WHERE id(f) = $id AND (f.status = 'OFFEN' OR (f.status = 'IN_ARBEIT' "
              + "AND f.inArbeitSeit < $grenze)) "
              + "SET f.status = 'IN_ARBEIT', f.bearbeitetVon = $wer, f.inArbeitSeit = $jetzt "
              + "RETURN count(f) AS n")
                .bind(id).to("id")
                .bind(wer).to("wer")
                .bind(jetzt).to("jetzt")
                .bind(jetzt.minusHours(UEBERNAHME_VERFALLS_STUNDEN)).to("grenze")
                .fetchAs(Long.class).one().orElse(0L);
        return n > 0;
    }

    /** Gibt eine Frage in Arbeit wieder frei (z.B. wenn der Agent nicht weiterkommt). */
    public Frage freigeben(Frage f, String wer, String grund) {
        f.setStatus(FrageStatus.OFFEN);
        f.setBearbeitetVon(null);
        f.setInArbeitSeit(null);
        if (grund != null && !grund.isBlank()) {
            f.getVerlauf().add(eintrag(wer, "Freigegeben", grund));
        }
        return frageRepository.save(f);
    }

    public Frage beantworten(Frage f, String antwort, String wer) {
        if (antwort == null || antwort.isBlank()) {
            throw new IllegalArgumentException("antwort fehlt");
        }
        f.setStatus(FrageStatus.BEANTWORTET);
        f.setAntwort(antwort.trim());
        f.setBeantwortetVon(wer);
        f.setBeantwortetAm(LocalDateTime.now());
        f.setInArbeitSeit(null);
        f.getVerlauf().add(eintrag(wer, "Antwort", antwort.trim()));
        return frageRepository.save(f);
    }

    public Frage nachhaken(Frage f, String nachfrage, String wer) {
        if (nachfrage == null || nachfrage.isBlank()) {
            throw new IllegalArgumentException("Nachfrage fehlt");
        }
        f.setStatus(FrageStatus.OFFEN);
        f.setNachfrage(nachfrage.trim());
        f.setBearbeitetVon(null);
        f.setInArbeitSeit(null);
        f.getVerlauf().add(eintrag(wer, "Nachfrage", nachfrage.trim()));
        return frageRepository.save(f);
    }

    public Frage setzeStatus(Frage f, FrageStatus status) {
        f.setStatus(status);
        if (status == FrageStatus.OFFEN) {
            f.setBearbeitetVon(null);
            f.setInArbeitSeit(null);
        }
        return frageRepository.save(f);
    }

    /** Loescht die Frage samt Frage-Aussage (und damit deren Verknuepfungen). Quellen-Aussagen bleiben. */
    public void loesche(Frage f) {
        client.query("MATCH (f:Frage) WHERE id(f) = $id "
                   + "OPTIONAL MATCH (f)-[:ALS_AUSSAGE]->(a:Aussage) DETACH DELETE f, a")
                .bind(f.getId()).to("id")
                .run();
    }

    /** ID der Frage, deren Frage-Aussage {@code aussageId} ist. */
    public Optional<Long> frageZuAussage(Long aussageId) {
        return client.query("MATCH (f:Frage)-[:ALS_AUSSAGE]->(a:Aussage) WHERE id(a) = $id RETURN id(f) AS id")
                .bind(aussageId).to("id")
                .fetchAs(Long.class).first();
    }

    /** Anzahl Verknuepfungen je Frage-ID (Kontext im Netz). */
    public Map<Long, Long> kontextAnzahl() {
        Map<Long, Long> ergebnis = new java.util.HashMap<>();
        client.query("MATCH (f:Frage)-[:ALS_AUSSAGE]->(a:Aussage) "
                   + "RETURN id(f) AS id, COUNT { (a)-[]-(:Aussage) } AS n")
                .fetch().all()
                .forEach(z -> ergebnis.put(((Number) z.get("id")).longValue(), ((Number) z.get("n")).longValue()));
        return ergebnis;
    }

    private static String eintrag(String wer, String art, String text) {
        return LocalDateTime.now().format(DATUM) + " · " + (wer != null ? wer : "?") + " · " + art + ": " + text;
    }

    private static String leerZuNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
