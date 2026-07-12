package de.witchcafe.knownet.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.BeziehungsArt;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.BeziehungService;
import de.witchcafe.knownet.service.SchlagwortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Import-Endpunkt fuer Batch-Upload von Quellen, Aussagen und Verknuepfungen.
 *
 * Format:
 * {
 *   "quellen": [{"ref":"q1","url":"...","titel":"...","autor":"...","schlagworte":"..."}],
 *   "aussagen": [{"ref":"a1","text":"...","schlagworte":"...","quelleRef":"q1","zitat":"...","fundstelle":"..."}],
 *   "verknuepfungen": [{"vonRef":"a1","zuRef":"a2","art":"BESTAETIGT","kommentar":"..."}]
 * }
 */
@RestController
@RequestMapping("/api/import")
public class ImportApiController {

    private static final Logger log = LoggerFactory.getLogger(ImportApiController.class);

    private final QuelleRepository quelleRepository;
    private final AussageRepository aussageRepository;
    private final BeziehungService beziehungService;
    private final SchlagwortService schlagwortService;
    private final ObjectMapper objectMapper;

    public ImportApiController(QuelleRepository quelleRepository,
                               AussageRepository aussageRepository,
                               BeziehungService beziehungService,
                               SchlagwortService schlagwortService,
                               ObjectMapper objectMapper) {
        this.quelleRepository = quelleRepository;
        this.aussageRepository = aussageRepository;
        this.beziehungService = beziehungService;
        this.schlagwortService = schlagwortService;
        this.objectMapper = objectMapper;
    }

    /** JSON direkt als Body */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<ImportErgebnis> importJson(@RequestBody ImportDaten daten) {
        return ResponseEntity.ok(verarbeite(daten));
    }

    /** Datei-Upload (multipart/form-data, field: "file") */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImportErgebnis> importDatei(@RequestParam("file") MultipartFile file) {
        try {
            ImportDaten daten = objectMapper.readValue(file.getInputStream(), ImportDaten.class);
            return ResponseEntity.ok(verarbeite(daten));
        } catch (Exception e) {
            log.error("Import fehlgeschlagen", e);
            return ResponseEntity.badRequest()
                    .body(new ImportErgebnis(0, 0, 0, List.of("Fehler beim Parsen: " + e.getMessage())));
        }
    }

    public ImportErgebnis verarbeite(ImportDaten daten) {
        Map<String, Long> quelleIds = new HashMap<>();
        Map<String, Long> aussageIds = new HashMap<>();
        List<String> fehler = new ArrayList<>();
        int quellen = 0, aussagen = 0, verknuepfungen = 0;

        // Quellen
        if (daten.quellen() != null) {
            for (var q : daten.quellen()) {
                try {
                    Quelle quelle = new Quelle(q.url(), q.titel(), q.autor());
                    if (q.schlagworte() != null)
                        quelle.setSchlagworte(schlagwortService.ausKommaListe(q.schlagworte()));
                    quelle = quelleRepository.save(quelle);
                    if (q.ref() != null) quelleIds.put(q.ref(), quelle.getId());
                    quellen++;
                } catch (Exception e) {
                    fehler.add("Quelle [" + q.ref() + "]: " + e.getMessage());
                }
            }
        }

        // Aussagen
        if (daten.aussagen() != null) {
            for (var a : daten.aussagen()) {
                try {
                    Aussage aussage = new Aussage(a.text());
                    if (a.schlagworte() != null)
                        aussage.setSchlagworte(schlagwortService.ausKommaListe(a.schlagworte()));
                    if (a.quelleRef() != null && quelleIds.containsKey(a.quelleRef())) {
                        quelleRepository.findById(quelleIds.get(a.quelleRef())).ifPresent(q ->
                            aussage.getQuellen().add(new StammtAus(q,
                                a.zitat() != null ? a.zitat() : "",
                                a.fundstelle() != null ? a.fundstelle() : "")));
                    }
                    Aussage gespeichert = aussageRepository.save(aussage);
                    if (a.ref() != null) aussageIds.put(a.ref(), gespeichert.getId());
                    aussagen++;
                } catch (Exception e) {
                    fehler.add("Aussage [" + a.ref() + "]: " + e.getMessage());
                }
            }
        }

        // Verknuepfungen
        if (daten.verknuepfungen() != null) {
            for (var v : daten.verknuepfungen()) {
                try {
                    Long vonId = aussageIds.get(v.vonRef());
                    Long zuId  = aussageIds.get(v.zuRef());
                    if (vonId == null) { fehler.add("vonRef '" + v.vonRef() + "' nicht gefunden"); continue; }
                    if (zuId  == null) { fehler.add("zuRef '"  + v.zuRef()  + "' nicht gefunden"); continue; }
                    BeziehungsArt art = BeziehungsArt.valueOf(v.art().toUpperCase());
                    beziehungService.verknuepfe(vonId, zuId, art,
                            v.kommentar() != null ? v.kommentar() : "");
                    verknuepfungen++;
                } catch (IllegalArgumentException e) {
                    fehler.add("Verknüpfung [" + v.vonRef() + "→" + v.zuRef() + "]: ungültige Art '" + v.art() + "'");
                } catch (Exception e) {
                    fehler.add("Verknüpfung [" + v.vonRef() + "→" + v.zuRef() + "]: " + e.getMessage());
                }
            }
        }

        log.info("Import: {} Quellen, {} Aussagen, {} Verknüpfungen, {} Fehler",
                quellen, aussagen, verknuepfungen, fehler.size());
        return new ImportErgebnis(quellen, aussagen, verknuepfungen, fehler);
    }

    public record ImportDaten(
            List<QuelleImport> quellen,
            List<AussageImport> aussagen,
            List<VerknuepfungImport> verknuepfungen) {}

    public record QuelleImport(
            String ref, String url, String titel, String autor, String schlagworte) {}

    public record AussageImport(
            String ref, String text, String schlagworte,
            String quelleRef, String zitat, String fundstelle) {}

    public record VerknuepfungImport(
            String vonRef, String zuRef, String art, String kommentar) {}

    public record ImportErgebnis(
            int quellenAngelegt,
            int aussagenAngelegt,
            int verknuepfungenAngelegt,
            List<String> fehler) {}
}
