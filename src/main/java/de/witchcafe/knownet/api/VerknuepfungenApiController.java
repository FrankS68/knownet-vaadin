package de.witchcafe.knownet.api;

import de.witchcafe.knownet.domain.BeziehungsArt;
import de.witchcafe.knownet.service.Beziehung;
import de.witchcafe.knownet.service.BeziehungService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/verknuepfungen")
public class VerknuepfungenApiController {

    private final BeziehungService beziehungService;

    public VerknuepfungenApiController(BeziehungService beziehungService) {
        this.beziehungService = beziehungService;
    }

    @GetMapping
    public Collection<Beziehung> alle() {
        return beziehungService.alleBeziehungen();
    }

    @PostMapping
    public ResponseEntity<String> anlegen(@RequestBody VerknuepfungRequest req) {
        try {
            BeziehungsArt art = BeziehungsArt.valueOf(req.art().toUpperCase());
            beziehungService.verknuepfe(req.vonAussageId(), req.zuAussageId(), art,
                    req.kommentar() != null ? req.kommentar() : "");
            return ResponseEntity.ok("Verknüpfung angelegt");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Ungültige Beziehungsart. Erlaubt: " +
                          java.util.Arrays.stream(BeziehungsArt.values())
                              .map(Enum::name).reduce((a, b) -> a + ", " + b).orElse(""));
        }
    }

    public record VerknuepfungRequest(
            Long vonAussageId,
            Long zuAussageId,
            String art,
            String kommentar) {}
}
