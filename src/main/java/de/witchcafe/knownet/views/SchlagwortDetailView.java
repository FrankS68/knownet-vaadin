package de.witchcafe.knownet.views;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.SchlagwortService;

/** Ein Schlagwort mit Ober-/Unterbegriffen und allen Quellen und Aussagen (inkl. Unterbegriffe). */
@Route("schlagwort")
@AnonymousAllowed
public class SchlagwortDetailView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {

    private static final Logger log = LoggerFactory.getLogger(SchlagwortDetailView.class);

    private final SchlagwortService schlagwortService;
    private final QuelleRepository quelleRepository;
    private final AussageRepository aussageRepository;

    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::lade);
    private final VerticalLayout inhalt = new VerticalLayout();
    private String name;
    private String titel = "Schlagwort";

    public SchlagwortDetailView(SchlagwortService schlagwortService, QuelleRepository quelleRepository,
                                AussageRepository aussageRepository) {
        this.schlagwortService = schlagwortService;
        this.quelleRepository = quelleRepository;
        this.aussageRepository = aussageRepository;
        inhalt.setPadding(false);
        inhalt.setSpacing(false);
        inhalt.setMaxWidth("60em");
        add(fehlerBanner, inhalt);
    }

    @Override
    public void setParameter(BeforeEvent event, String name) {
        this.name = name;
        lade();
    }

    @Override
    public String getPageTitle() {
        return titel + " | Knownet";
    }

    private void lade() {
        inhalt.removeAll();
        try {
            Optional<Schlagwort> schlagwort = schlagwortService.finde(name);
            if (schlagwort.isEmpty()) {
                inhalt.add(new H2("Schlagwort nicht gefunden"),
                        new RouterLink("Zu allen Schlagworten", SchlagworteView.class));
            } else {
                zeige(schlagwort.get().getName());
            }
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            fehlerBanner.setVisible(true);
        }
    }

    private void zeige(String sw) {
        titel = sw;
        inhalt.add(new RouterLink("← Alle Schlagworte", SchlagworteView.class), new H2(sw));
        inhalt.add(Verweise.feld("Oberbegriffe", Verweise.schlagworte(schlagwortService.oberbegriffe(sw))));
        inhalt.add(Verweise.feld("Unterbegriffe", Verweise.schlagworte(schlagwortService.unterbegriffe(sw))));

        List<Quelle> quellen = quelleRepository
                .findAllById(schlagwortService.idsMitSchlagwortOderUnterbegriff("Quelle", sw)).stream()
                .sorted(Comparator.comparing(q -> q.toString().toLowerCase()))
                .toList();
        inhalt.add(new H3("Quellen (" + quellen.size() + ")"));
        if (quellen.isEmpty()) {
            inhalt.add(new Paragraph("Keine Quellen mit diesem Schlagwort oder einem Unterbegriff."));
        }
        for (Quelle q : quellen) {
            inhalt.add(Verweise.eintrag(Verweise.quelle(q),
                    Verweise.schlagworte(q.getSchlagworte().stream().map(Schlagwort::getName).toList())));
        }

        List<Aussage> aussagen = aussageRepository
                .findAllById(schlagwortService.idsMitSchlagwortOderUnterbegriff("Aussage", sw)).stream()
                .sorted(Comparator.comparing(a -> a.getText() != null ? a.getText().toLowerCase() : ""))
                .toList();
        inhalt.add(new H3("Aussagen (" + aussagen.size() + ")"));
        if (aussagen.isEmpty()) {
            inhalt.add(new Paragraph("Keine Aussagen mit diesem Schlagwort oder einem Unterbegriff."));
        }
        for (Aussage a : aussagen) {
            inhalt.add(Verweise.eintrag(Verweise.aussage(a.getId(), a.getText()),
                    Verweise.schlagworte(a.getSchlagworte().stream().map(Schlagwort::getName).toList())));
        }
        inhalt.add(Verweise.grau("Enthält auch Einträge, die nur einen Unterbegriff tragen."));
    }
}
