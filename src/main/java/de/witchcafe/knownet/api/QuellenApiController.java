package de.witchcafe.knownet.api;

import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.ErstellerService;
import de.witchcafe.knownet.service.SchlagwortService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quellen")
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
    public List<Quelle> alle() {
        return quelleRepository.findAll();
    }

    @PostMapping
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
