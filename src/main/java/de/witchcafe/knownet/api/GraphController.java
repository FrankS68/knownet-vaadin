package de.witchcafe.knownet.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;

import de.witchcafe.knownet.service.GraphService;
import de.witchcafe.knownet.service.GraphService.GraphData;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    // Öffentlicher Endpoint - wird durch SecurityFilterChain mit @Order(0) erlaubt
    public GraphData graph(@RequestParam(required = false) String suche) {
        return graphService.ladeGraph(suche);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GraphData> handleException(Exception e) {
        return ResponseEntity.ok(new GraphData(
                java.util.List.of(),
                java.util.List.of(),
                "Fehler beim Laden des Graphen: " + e.getMessage()
        ));
    }
}
