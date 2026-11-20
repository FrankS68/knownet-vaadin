package de.witchcafe.knownet.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.knownet.service.SchlagwortService;
import de.witchcafe.knownet.service.SchlagwortService.Eintrag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Schlagworte als Themenliste fuer Agenten: jedes Schlagwort mit Ober- und Unterbegriffen,
 * allen vollstaendigen Pfaden von der Wurzel (direkt als {@code schlagworte} beim Import verwendbar)
 * und Nutzungszahlen.
 */
@RestController
@RequestMapping("/api/schlagwoerter")
@Tag(name = "Schlagwoerter", description = "Hierarchische Schlagworte (Themen)")
public class SchlagwoerterApiController {

    private final SchlagwortService schlagwortService;

    public SchlagwoerterApiController(SchlagwortService schlagwortService) {
        this.schlagwortService = schlagwortService;
    }

    public record SchlagwortDto(String name, List<String> oberbegriffe, List<String> unterbegriffe,
            List<String> pfade, long quellen, long aussagen) {}

    @GetMapping
    @Operation(summary = "Alle Schlagworte mit Ober-/Unterbegriffen, Pfaden ab der Wurzel und Nutzungszahlen")
    public List<SchlagwortDto> alle() {
        List<Eintrag> eintraege = schlagwortService.alleEintraege();
        Map<String, List<String>> ober = new HashMap<>();
        Map<String, Set<String>> unter = new HashMap<>();
        for (Eintrag e : eintraege) {
            ober.put(e.name(), e.oberbegriffe());
            for (String o : e.oberbegriffe()) {
                unter.computeIfAbsent(o, k -> new TreeSet<>()).add(e.name());
            }
        }
        List<SchlagwortDto> ergebnis = new ArrayList<>();
        for (Eintrag e : eintraege) {
            ergebnis.add(new SchlagwortDto(e.name(),
                    e.oberbegriffe().stream().sorted().toList(),
                    new ArrayList<>(unter.getOrDefault(e.name(), Set.of())),
                    new ArrayList<>(pfade(e.name(), ober, new HashSet<>())),
                    e.quellen(), e.aussagen()));
        }
        return ergebnis;
    }

    /** Alle Pfade von einer Wurzel bis {@code name}; {@code besucht} schuetzt vor Zyklen in Altdaten. */
    private static Set<String> pfade(String name, Map<String, List<String>> ober, Set<String> besucht) {
        Set<String> ergebnis = new TreeSet<>();
        List<String> eltern = ober.getOrDefault(name, List.of());
        if (eltern.isEmpty() || !besucht.add(name)) {
            ergebnis.add(name);
            return ergebnis;
        }
        for (String o : eltern) {
            for (String p : pfade(o, ober, besucht)) {
                ergebnis.add(p + SchlagwortService.PFAD_TRENNER + name);
            }
        }
        besucht.remove(name);
        return ergebnis;
    }
}
