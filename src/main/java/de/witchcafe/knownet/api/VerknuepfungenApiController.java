package de.witchcafe.knownet.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import de.witchcafe.knownet.domain.BeziehungsArt;
import de.witchcafe.knownet.service.Beziehung;
import de.witchcafe.knownet.service.BeziehungService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/verknuepfungen")
@Tag(name = "Verknuepfungen", description = "Typisierte Beziehungen zwischen Aussagen (Arten siehe BeziehungsArt)")
public class VerknuepfungenApiController {

    private final BeziehungService beziehungService;

    public VerknuepfungenApiController(BeziehungService beziehungService) {
        this.beziehungService = beziehungService;
    }

    @GetMapping
    @Operation(summary = "Alle Verknuepfungen, neueste zuerst")
    public Collection<Beziehung> alle() {
        return beziehungService.alleBeziehungen();
    }

    @PostMapping
    @Operation(summary = "Zwei Aussagen verknuepfen",
            description = "art: BESTAETIGT, WIDERSPRICHT, WIDERLEGT, ERWEITERT, PRAEZISIERT, RELATIVIERT, GEFAEHRDET, "
                    + "SETZT_VORAUS, FOLGT_AUS, VERANSCHAULICHT, ERSETZT, BEANTWORTET, BETRIFFT")
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
