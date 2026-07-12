package de.witchcafe.knownet.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "graph", layout = MainLayout.class)
@PageTitle("Graph | Knownet")
@AnonymousAllowed
public class GraphView extends VerticalLayout {

    private final TextField suchFeld = new TextField();

    public GraphView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        suchFeld.setPlaceholder("Suche nach Aussage, Quelle oder Schlagwort ...");
        suchFeld.setWidth("30em");
        suchFeld.setClearButtonVisible(true);

        Button suchen = new Button("Filtern", e -> ladeGraph(suchFeld.getValue().trim()));
        suchen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button alles = new Button("Alles zeigen", e -> {
            suchFeld.clear();
            ladeGraph("");
        });

        HorizontalLayout toolbar = new HorizontalLayout(suchFeld, suchen, alles);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();

        Div graphDiv = new Div();
        graphDiv.setId("kn-graph");
        graphDiv.getStyle()
                .set("width", "100%")
                .set("height", "100%")
                .set("min-height", "600px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-base-color)");

        add(toolbar, graphDiv);
        expand(graphDiv);

        getElement().executeJs("""
            function knLoadVis(callback) {
                if (window.vis && window.vis.Network) { callback(); return; }
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = 'https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.9/dist/dist/vis-network.min.css';
                document.head.appendChild(link);
                const script = document.createElement('script');
                script.src = 'https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.9/dist/vis-network.min.js';
                script.onload = callback;
                document.head.appendChild(script);
            }
            knLoadVis(function() {
                window.knRenderGraph = function(apiUrl) {
                    fetch(apiUrl)
                        .then(r => r.json())
                        .then(function(data) {
                            var container = document.getElementById('kn-graph');
                            if (!container) return;
                            var nodeMap = {};
                            var visNodes = data.nodes.map(function(n) {
                                nodeMap[n.id] = true;
                                var shape = n.typ === 'Schlagwort' ? 'ellipse'
                                          : n.typ === 'Quelle'     ? 'box'
                                          : 'roundrectangle';
                                return {
                                    id: n.id, label: n.label, title: n.title,
                                    shape: shape,
                                    color: { background: n.color, border: n.color,
                                             highlight: { background: n.color, border: '#333' } },
                                    font: { color: '#fff', size: 13 },
                                    margin: 8
                                };
                            });
                            var visEdges = data.edges
                                .filter(function(e) { return nodeMap[e.from] && nodeMap[e.to]; })
                                .map(function(e) {
                                    return {
                                        id: e.id, from: e.from, to: e.to,
                                        label: e.label, title: e.title,
                                        arrows: 'to',
                                        font: { size: 11, align: 'middle' },
                                        color: { color: '#aaa', highlight: '#555' }
                                    };
                                });
                            var options = {
                                physics: {
                                    solver: 'forceAtlas2Based',
                                    forceAtlas2Based: { gravitationalConstant: -60, springLength: 140 },
                                    stabilization: { iterations: 250 }
                                },
                                interaction: { hover: true, tooltipDelay: 100 },
                                layout: { improvedLayout: true }
                            };
                            if (window.knNetwork) { window.knNetwork.destroy(); }
                            window.knNetwork = new vis.Network(
                                container,
                                { nodes: new vis.DataSet(visNodes), edges: new vis.DataSet(visEdges) },
                                options
                            );
                        })
                        .catch(function(err) { console.error('Graph-Fehler:', err); });
                };
                window.knRenderGraph('/api/graph');
            });
        """);
    }

    private void ladeGraph(String suche) {
        String url = "/api/graph" + (suche.isBlank() ? "" : "?suche=" + suche);
        getElement().executeJs("if(window.knRenderGraph) window.knRenderGraph($0);", url);
    }
}
