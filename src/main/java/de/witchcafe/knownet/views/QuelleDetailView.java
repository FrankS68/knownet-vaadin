package de.witchcafe.knownet.views;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.ZusammenhangService;
import de.witchcafe.knownet.service.ZusammenhangService.AussageBeziehung;

/** Eine Quelle mit allen Aussagen, die aus ihr stammen, und deren Beziehungen. */
@Route("quelle")
@AnonymousAllowed
public class QuelleDetailView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private static final Logger log = LoggerFactory.getLogger(QuelleDetailView.class);
    static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final QuelleRepository quelleRepository;
    private final AussageRepository aussageRepository;
    private final ZusammenhangService zusammenhang;

    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::lade);
    private final VerticalLayout inhalt = new VerticalLayout();
    private Long id;
    private String titel = "Quelle";

    public QuelleDetailView(QuelleRepository quelleRepository, AussageRepository aussageRepository,
                            ZusammenhangService zusammenhang) {
        this.quelleRepository = quelleRepository;
        this.aussageRepository = aussageRepository;
        this.zusammenhang = zusammenhang;
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
            Optional<Quelle> quelle = quelleRepository.findById(id);
            if (quelle.isEmpty()) {
                inhalt.add(new H2("Quelle nicht gefunden"), new RouterLink("Zu allen Quellen", QuellenView.class));
            } else {
                zeige(quelle.get());
            }
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            fehlerBanner.setVisible(true);
        }
    }

    private void zeige(Quelle q) {
        titel = q.toString();
        inhalt.add(new RouterLink("← Alle Quellen", QuellenView.class), new H2(titel));

        inhalt.add(Verweise.feld("URL", Verweise.extern(q.getUrl())));
        if (q.getAutor() != null && !q.getAutor().isBlank()) {
            inhalt.add(Verweise.feld("Autor", new Span(q.getAutor())));
        }
        String erfasst = (q.getErfasstAm() != null ? q.getErfasstAm().format(DATUM) : "–")
                + (q.getErstelltVon() != null ? " von " + q.getErstelltVon() : "");
        inhalt.add(Verweise.feld("Erfasst", new Span(erfasst)));
        inhalt.add(Verweise.feld("KI-Wahrscheinlichkeit", kiText(q)));
        inhalt.add(Verweise.feld("Schlagworte",
                Verweise.schlagworte(q.getSchlagworte().stream().map(Schlagwort::getName).toList())));

        List<Aussage> aussagen = aussageRepository.findAllById(zusammenhang.aussageIdsZuQuelle(q.getId()))
                .stream()
                .sorted(Comparator.comparing(Aussage::getErstelltAm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .toList();
        Map<Long, List<AussageBeziehung>> beziehungen =
                zusammenhang.beziehungenVon(aussagen.stream().map(Aussage::getId).toList());

        inhalt.add(new H3("Aussagen aus dieser Quelle (" + aussagen.size() + ")"));
        if (aussagen.isEmpty()) {
            inhalt.add(new Paragraph("Noch keine Aussagen erfasst."));
        }
        for (Aussage a : aussagen) {
            StammtAus herkunft = a.getQuellen().stream()
                    .filter(s -> s.getQuelle() != null && q.getId().equals(s.getQuelle().getId()))
                    .findFirst().orElse(null);
            var eintrag = Verweise.eintrag(
                    Verweise.aussage(a.getId(), a.getText()),
                    herkunft != null ? Verweise.zitat(herkunft.getZitat(), herkunft.getFundstelle()) : null);
            Verweise.beziehungen(beziehungen.getOrDefault(a.getId(), List.of()), "diese Aussage")
                    .forEach(eintrag::add);
            inhalt.add(eintrag);
        }
    }

    private static Span kiText(Quelle q) {
        if (q.getKiWahrscheinlichkeit() == null) {
            return Verweise.grau("noch nicht bewertet");
        }
        StringBuilder sb = new StringBuilder(q.getKiWahrscheinlichkeit() + " %");
        if (q.getKiBegruendung() != null && !q.getKiBegruendung().isBlank()) {
            sb.append(" – ").append(q.getKiBegruendung());
        }
        if (q.getKiBewertetVon() != null) {
            sb.append(" (").append(q.getKiBewertetVon());
            if (q.getKiBewertetAm() != null) {
                sb.append(", ").append(q.getKiBewertetAm().format(DATUM));
            }
            sb.append(")");
        }
        return new Span(sb.toString());
    }
}
