package de.witchcafe.knownet.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.knownet.domain.Frage;
import de.witchcafe.knownet.domain.FrageStatus;
import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.service.ErstellerService;
import de.witchcafe.knownet.service.FrageService;
import de.witchcafe.knownet.service.ZusammenhangService;
import de.witchcafe.knownet.service.ZusammenhangService.AussageBeziehung;

/**
 * Recherchefragen fuer Agenten.
 * <p>
 * Ablauf fuer einen Agenten:
 * <ol>
 *   <li>{@code GET /api/fragen/offen} – Fragen, die er uebernehmen darf (aelteste zuerst).</li>
 *   <li>{@code POST /api/fragen/{id}/uebernehmen} – reserviert die Frage (409, wenn schon vergeben).</li>
 *   <li>Quellen suchen, per {@code POST /api/import} Quellen und Aussagen anlegen und die Aussagen
 *       mit der Frage-Aussage ({@code aussageId}) verknuepfen: {@code verknuepfungen[].zuId = aussageId},
 *       Art z.B. BEANTWORTET, BESTAETIGT, WIDERSPRICHT, RELATIVIERT oder BETRIFFT.</li>
 *   <li>{@code PUT /api/fragen/{id}/antwort} mit einer Zusammenfassung – oder
 *       {@code POST /api/fragen/{id}/freigeben}, wenn er nicht weiterkommt.</li>
 * </ol>
 * Hat der Benutzer nachgehakt, steht die Frage wieder auf OFFEN; {@code nachfrage} und {@code verlauf}
 * zeigen, was noch fehlt.
 */
@RestController
@RequestMapping("/api/fragen")
@Tag(name = "Fragen", description = "Recherchefragen von Benutzern an Agenten: uebernehmen, Kontext verknuepfen, beantworten")
public class FragenApiController {

    private final FrageService frageService;
    private final ZusammenhangService zusammenhang;
    private final ErstellerService erstellerService;

    public FragenApiController(FrageService frageService, ZusammenhangService zusammenhang,
                               ErstellerService erstellerService) {
        this.frageService = frageService;
        this.zusammenhang = zusammenhang;
        this.erstellerService = erstellerService;
    }

    /** Alle Fragen, optional gefiltert: {@code ?status=OFFEN|IN_ARBEIT|BEANTWORTET|ERLEDIGT}. */
    @GetMapping
    @Operation(summary = "Alle Fragen, neueste zuerst, optional nach Status gefiltert")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FrageDto.class))))
    public ResponseEntity<?> alle(@RequestParam(required = false) String status) {
        FrageStatus s = null;
        if (status != null && !status.isBlank()) {
            try {
                s = FrageStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Unbekannter Status. Erlaubt: OFFEN, IN_ARBEIT, BEANTWORTET, ERLEDIGT");
            }
        }
        return ResponseEntity.ok(alsDtos(frageService.alle(s)));
    }

    /** Fragen, die der aufrufende Agent uebernehmen darf (offen oder seit 24 h haengengeblieben). */
    @GetMapping("/offen")
    @Operation(summary = "Fragen, die der aufrufende Agent uebernehmen darf (aelteste zuerst)",
            description = "OFFEN und zustaendig leer oder Anfang des eigenen Key-Namens; IN_ARBEIT nach 24 h ebenfalls.")
    public List<FrageDto> offen() {
        return alsDtos(frageService.uebernehmbarFuer(agentName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Eine Frage mit Antwort, Nachfrage, Verlauf und Kontext (verknuepfte Aussagen)")
    public ResponseEntity<FrageDto> eine(@PathVariable Long id) {
        return frageService.finde(id)
                .map(f -> ResponseEntity.ok(alsDtos(List.of(f)).get(0)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Auch Agenten duerfen Fragen stellen (z.B. an einen anderen Agenten). */
    @PostMapping
    @Operation(summary = "Frage stellen (legt auch die Frage-Aussage an)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FrageDto.class)))
    public ResponseEntity<?> stellen(@RequestBody FrageRequest req) {
        if (req.text() == null || req.text().isBlank()) {
            return ResponseEntity.badRequest().body("text fehlt");
        }
        Frage f = frageService.stellen(req.text(), req.hintergrund(), req.schlagworte(), req.zustaendig(),
                erstellerService.aktuellerErsteller());
        return ResponseEntity.ok(alsDtos(List.of(f)).get(0));
    }

    @PostMapping("/{id}/uebernehmen")
    @Operation(summary = "Frage reservieren (Status IN_ARBEIT)",
            description = "409: nicht offen bzw. schon vergeben; 403: an einen anderen Agenten gerichtet.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FrageDto.class)))
    public ResponseEntity<?> uebernehmen(@PathVariable Long id) {
        var frage = frageService.finde(id);
        if (frage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!FrageService.istZustaendig(frage.get(), agentName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Frage ist an \"" + frage.get().getZustaendig() + "\" gerichtet");
        }
        if (!frageService.uebernehmen(id, erstellerService.aktuellerErsteller())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Frage ist nicht offen (Status: " + frage.get().getStatus().name()
                          + (frage.get().getBearbeitetVon() != null ? ", bearbeitet von " + frage.get().getBearbeitetVon() : "")
                          + ")");
        }
        return eine(id);
    }

    @PostMapping("/{id}/freigeben")
    @Operation(summary = "Frage in Arbeit wieder freigeben (Status OFFEN), optional mit Grund")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FrageDto.class)))
    public ResponseEntity<?> freigeben(@PathVariable Long id, @RequestBody(required = false) FreigabeRequest req) {
        return frageService.finde(id)
                .<ResponseEntity<?>>map(f -> {
                    if (f.getStatus() != FrageStatus.IN_ARBEIT) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Frage ist nicht in Arbeit");
                    }
                    frageService.freigeben(f, erstellerService.aktuellerErsteller(), req != null ? req.grund() : null);
                    return eine(id);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/antwort")
    @Operation(summary = "Antwort (Zusammenfassung) speichern, Status BEANTWORTET",
            description = "Vorher die Aussagen der Quellen per POST /api/import mit der Frage-Aussage (aussageId) verknuepfen.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FrageDto.class)))
    public ResponseEntity<?> antwort(@PathVariable Long id, @RequestBody AntwortRequest req) {
        if (req.antwort() == null || req.antwort().isBlank()) {
            return ResponseEntity.badRequest().body("antwort fehlt");
        }
        return frageService.finde(id)
                .<ResponseEntity<?>>map(f -> {
                    if (f.getStatus() == FrageStatus.ERLEDIGT) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Frage ist bereits erledigt");
                    }
                    frageService.beantworten(f, req.antwort(), erstellerService.aktuellerErsteller());
                    return eine(id);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private List<FrageDto> alsDtos(List<Frage> fragen) {
        List<Long> aussageIds = fragen.stream()
                .filter(f -> f.getAussage() != null)
                .map(f -> f.getAussage().getId())
                .toList();
        Map<Long, List<AussageBeziehung>> kontext = zusammenhang.beziehungenVon(aussageIds);
        return fragen.stream().map(f -> {
            Long aussageId = f.getAussage() != null ? f.getAussage().getId() : null;
            List<String> schlagworte = f.getAussage() != null
                    ? f.getAussage().getSchlagworte().stream().map(Schlagwort::getName).sorted().toList()
                    : List.of();
            List<KontextDto> k = aussageId == null ? List.of()
                    : kontext.getOrDefault(aussageId, List.of()).stream()
                        .map(b -> new KontextDto(b.andereId(), b.andererText(), b.art(),
                                b.ausgehend() ? "frage→aussage" : "aussage→frage", b.kommentar()))
                        .toList();
            return new FrageDto(f.getId(), f.getText(), f.getHintergrund(), aussageId, schlagworte,
                    f.getZustaendig(), f.getStatus().name(), f.getGestelltVon(), f.getGestelltAm(),
                    f.getBearbeitetVon(), f.getInArbeitSeit(), f.getAntwort(), f.getBeantwortetVon(),
                    f.getBeantwortetAm(), f.getNachfrage(), f.getVerlauf(), k);
        }).toList();
    }

    /** Name des API-Keys (bzw. des angemeldeten Benutzers), gegen {@code zustaendig} geprueft. */
    private static String agentName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public record FrageRequest(String text, String hintergrund, String schlagworte, String zustaendig) {}

    public record FreigabeRequest(String grund) {}

    public record AntwortRequest(String antwort) {}

    /** Eine mit der Frage-Aussage verknuepfte Aussage. */
    public record KontextDto(Long aussageId, String text, String art, String richtung, String kommentar) {}

    public record FrageDto(Long id, String text, String hintergrund, Long aussageId, List<String> schlagworte,
                           String zustaendig, String status, String gestelltVon, LocalDateTime gestelltAm,
                           String bearbeitetVon, LocalDateTime inArbeitSeit, String antwort,
                           String beantwortetVon, LocalDateTime beantwortetAm, String nachfrage,
                           List<String> verlauf, List<KontextDto> kontext) {}
}
