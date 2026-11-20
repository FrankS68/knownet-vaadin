package de.witchcafe.knownet.views;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.service.FrageService;
import de.witchcafe.knownet.service.ZusammenhangService;
import de.witchcafe.knownet.service.ZusammenhangService.AussageBeziehung;

/** Eine Aussage mit ihren Quellen, Schlagworten und Beziehungen zu anderen Aussagen. */
@Route("aussage")
@AnonymousAllowed
public class AussageDetailView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private static final Logger log = LoggerFactory.getLogger(AussageDetailView.class);

    private final AussageRepository aussageRepository;
    private final ZusammenhangService zusammenhang;
    private final FrageService frageService;

    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::lade);
    private final VerticalLayout inhalt = new VerticalLayout();
    private Long id;
    private String titel = "Aussage";

    public AussageDetailView(AussageRepository aussageRepository, ZusammenhangService zusammenhang,
                             FrageService frageService) {
        this.aussageRepository = aussageRepository;
        this.zusammenhang = zusammenhang;
        this.frageService = frageService;
        inhalt.setPadding(false);
        inhalt.setSpacing(false);
        inhalt.setMaxWidth("60em");
        add(fehlerBanner, inhalt);
    }

    @Override
    public void setParameter(BeforeEvent event, Long id) {
        this.id = id;
        lade();
    }

    @Override
    public String getPageTitle() {
        return titel + " | Knownet";
    }

    private void lade() {
        inhalt.removeAll();
        try {
            Optional<Aussage> aussage = aussageRepository.findById(id);
            if (aussage.isEmpty()) {
                inhalt.add(new H2("Aussage nicht gefunden"), new RouterLink("Zu allen Aussagen", AussagenView.class));
            } else {
                zeige(aussage.get());
            }
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            fehlerBanner.setVisible(true);
        }
    }

    private void zeige(Aussage a) {
        titel = Verweise.kuerze(a.getText(), 60);
        Paragraph text = new Paragraph(a.getText());
        text.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0.3em 0 0.8em 0");
        inhalt.add(new RouterLink("← Alle Aussagen", AussagenView.class), new H2("Aussage"), text);
        frageService.frageZuAussage(a.getId()).ifPresent(frageId ->
                inhalt.add(Verweise.feld("Recherchefrage",
                        new RouterLink("Diese Aussage ist eine Frage an die Agenten – Frage ansehen",
                                FrageDetailView.class, frageId))));

        if (a.getErstelltAm() != null) {
            inhalt.add(Verweise.feld("Erfasst", new Span(a.getErstelltAm().format(QuelleDetailView.DATUM))));
        }
        inhalt.add(Verweise.feld("Schlagworte",
                Verweise.schlagworte(a.getSchlagworte().stream().map(Schlagwort::getName).toList())));

        inhalt.add(new H3("Quellen (" + a.getQuellen().size() + ")"));
        if (a.getQuellen().isEmpty()) {
            inhalt.add(new Paragraph("Keine Quelle angegeben."));
        }
        for (StammtAus s : a.getQuellen()) {
            if (s.getQuelle() == null) {
                continue;
            }
            inhalt.add(Verweise.eintrag(
                    Verweise.quelle(s.getQuelle()),
                    Verweise.zitat(s.getZitat(), s.getFundstelle())));
        }

        List<AussageBeziehung> beziehungen = zusammenhang.beziehungenVon(a.getId());
        inhalt.add(new H3("Beziehungen zu anderen Aussagen (" + beziehungen.size() + ")"));
        if (beziehungen.isEmpty()) {
            inhalt.add(new Paragraph("Noch keine Verknüpfungen."));
        }
        Verweise.beziehungen(beziehungen, "diese Aussage")
                .forEach(c -> inhalt.add(Verweise.eintrag(c)));
    }
}
