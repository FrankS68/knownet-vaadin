package de.witchcafe.knownet.api;

import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.SchlagwortService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quellen")
public class QuellenApiController {

    private final QuelleRepository quelleRepository;
    private final SchlagwortService schlagwortService;

    public QuellenApiController(QuelleRepository quelleRepository,
                                SchlagwortService schlagwortService) {
        this.quelleRepository = quelleRepository;
        this.schlagwortService = schlagwortService;
    }

    @GetMapping
    public List<Quelle> alle() {
        return quelleRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Quelle> anlegen(@RequestBody QuelleRequest req) {
        Quelle q = new Quelle(req.url(), req.titel(), req.autor());
        if (req.schlagworte() != null) {
            q.setSchlagworte(schlagwortService.ausKommaListe(req.schlagworte()));
        }
        return ResponseEntity.ok(quelleRepository.save(q));
    }

    public record QuelleRequest(String url, String titel, String autor, String schlagworte) {}
}
