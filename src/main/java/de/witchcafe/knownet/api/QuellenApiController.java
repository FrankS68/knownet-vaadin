package de.witchcafe.knownet.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.ErstellerService;
import de.witchcafe.knownet.service.SchlagwortService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quellen")
@Tag(name = "Quellen", description = "Quellen (URL, Titel, Autor, Schlagworte, KI-Bewertung)")
public class QuellenApiController {

    private final QuelleRepository quelleRepository;
    private final SchlagwortService schlagwortService;
    private final ErstellerService erstellerService;

    public QuellenApiController(QuelleRepository quelleRepository,
                                SchlagwortService schlagwortService,
                                ErstellerService erstellerService) {
        this.quelleRepository = quelleRepository;
        this.schlagwortService = schlagwortService;
        this.erstellerService = erstellerService;
    }

    @GetMapping
    @Operation(summary = "Alle Quellen")
    public List<Quelle> alle() {
        return quelleRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Quelle anlegen", description = "schlagworte kommasepariert, Hierarchie mit /")
    public ResponseEntity<Quelle> anlegen(@RequestBody QuelleRequest req) {
        Quelle q = new Quelle(req.url(), req.titel(), req.autor());
        q.setErstelltVon(erstellerService.aktuellerErsteller());
        if (req.schlagworte() != null) {
            q.setSchlagworte(schlagwortService.ausKommaListe(req.schlagworte()));
        }
        return ResponseEntity.ok(quelleRepository.save(q));
    }

    /** Setzt oder ersetzt die Einschaetzung, wie wahrscheinlich die Quelle KI-generiert ist. */
    @PutMapping("/{id}/ki-bewertung")
    @Operation(summary = "KI-Wahrscheinlichkeit der Quelle setzen (0-100 mit Begruendung)")
    public ResponseEntity<?> kiBewertung(@PathVariable Long id, @RequestBody KiBewertungRequest req) {
        if (req.wahrscheinlichkeit() == null || req.wahrscheinlichkeit() < 0 || req.wahrscheinlichkeit() > 100) {
            return ResponseEntity.badRequest().body("wahrscheinlichkeit muss zwischen 0 und 100 liegen");
        }
        if (req.begruendung() == null || req.begruendung().isBlank()) {
            return ResponseEntity.badRequest().body("begruendung fehlt");
        }
        return quelleRepository.findById(id)
                .<ResponseEntity<?>>map(q -> {
                    q.setzeKiBewertung(req.wahrscheinlichkeit(), req.begruendung().trim(),
                            erstellerService.aktuellerErsteller());
                    return ResponseEntity.ok(quelleRepository.save(q));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record KiBewertungRequest(Integer wahrscheinlichkeit, String begruendung) {}

    public record QuelleRequest(String url, String titel, String autor, String schlagworte) {}
}
