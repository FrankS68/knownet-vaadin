package de.witchcafe.knownet.api;

import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.SchlagwortService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aussagen")
public class AussagenApiController {

    private final AussageRepository aussageRepository;
    private final QuelleRepository quelleRepository;
    private final SchlagwortService schlagwortService;

    public AussagenApiController(AussageRepository aussageRepository,
                                  QuelleRepository quelleRepository,
                                  SchlagwortService schlagwortService) {
        this.aussageRepository = aussageRepository;
        this.quelleRepository = quelleRepository;
        this.schlagwortService = schlagwortService;
    }

    @GetMapping
    public List<Aussage> alle() {
        return aussageRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Aussage> anlegen(@RequestBody AussageRequest req) {
        Aussage a = new Aussage(req.text());
        if (req.schlagworte() != null) {
            a.setSchlagworte(schlagwortService.ausKommaListe(req.schlagworte()));
        }
        if (req.quelleId() != null) {
            quelleRepository.findById(req.quelleId()).ifPresent(q ->
                a.getQuellen().add(new StammtAus(q,
                        req.zitat() != null ? req.zitat() : "",
                        req.fundstelle() != null ? req.fundstelle() : ""))
            );
        }
        return ResponseEntity.ok(aussageRepository.save(a));
    }

    public record AussageRequest(
            String text,
            String schlagworte,
            Long quelleId,
            String zitat,
            String fundstelle) {}
}
