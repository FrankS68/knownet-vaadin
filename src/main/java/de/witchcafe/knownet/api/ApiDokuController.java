package de.witchcafe.knownet.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.knownet.api.ApiAenderungen.Eintrag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Einstieg in die API-Doku fuer Agenten (oeffentlich, ohne API-Key).
 * <ul>
 *   <li>{@code GET /api/docs} – aktuelle Version und Links (billig, fuer den Versions-Check zu Beginn jedes Laufs)</li>
 *   <li>{@code GET /api/docs/anleitung} – Arbeitsanleitung als Markdown</li>
 *   <li>{@code GET /api/docs/aenderungen} – Aenderungsprotokoll, neueste zuerst</li>
 *   <li>{@code GET /api/docs/openapi.json} – vollstaendige, aus dem Code erzeugte OpenAPI-Beschreibung (springdoc)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/docs")
@Tag(name = "Doku", description = "API-Doku, Version und Aenderungsprotokoll (ohne API-Key)")
public class ApiDokuController {

    @GetMapping
    @Operation(summary = "Aktuelle API-Version und Links zur Doku",
            description = "Agenten rufen das zu Beginn jedes Laufs auf und vergleichen 'version' mit der gemerkten. "
                    + "Ist sie neu, 'aenderungen' lesen und Werkzeuge anpassen.")
    public Map<String, Object> index() {
        Eintrag neuester = ApiAenderungen.PROTOKOLL.get(0);
        return Map.of(
                "version", neuester.version(),
                "stand", neuester.datum(),
                "letzteAenderungInkompatibel", neuester.inkompatibel(),
                "anleitung", "/api/docs/anleitung",
                "aenderungen", "/api/docs/aenderungen",
                "openapi", "/api/docs/openapi.json",
                "authentifizierung", "Header 'Authorization: Bearer <API-Key>' fuer alle /api/** ausser /api/docs und /api/graph");
    }

    @GetMapping("/aenderungen")
    @Operation(summary = "Aenderungsprotokoll der API, neueste zuerst")
    public List<Eintrag> aenderungen() {
        return ApiAenderungen.PROTOKOLL;
    }

    @GetMapping(value = "/anleitung", produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Arbeitsanleitung fuer Agenten (Markdown)")
    public ResponseEntity<String> anleitung() throws IOException {
        try (InputStream in = new ClassPathResource("api-doku/anleitung.md").getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{version}}", ApiAenderungen.aktuelleVersion());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                    .body(text);
        }
    }
}
