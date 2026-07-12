package de.witchcafe.knownet.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public GraphData graph(@RequestParam(required = false) String suche) {
        return graphService.ladeGraph(suche);
    }
}
