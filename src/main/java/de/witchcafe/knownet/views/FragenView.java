package de.witchcafe.knownet.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.domain.ApiKey;
import de.witchcafe.knownet.domain.Frage;
import de.witchcafe.knownet.domain.FrageStatus;
import de.witchcafe.knownet.repo.ApiKeyRepository;
import de.witchcafe.knownet.service.ErstellerService;
import de.witchcafe.knownet.service.FrageService;

/** Recherchefragen an die Agenten: stellen und verfolgen. */
@Route("fragen")
@Menu(order = 0.5, icon = "vaadin:question-circle-o", title = "Fragen")
@PageTitle("Fragen | Knownet")
@AnonymousAllowed
public class FragenView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(FragenView.class);
    /** Filterwert für "alles außer erledigt". */
    private static final String NICHT_ERLEDIGT = "alle außer erledigt";
    private static final String ALLE = "alle";

    private final FrageService frageService;
    private final ApiKeyRepository apiKeyRepository;
    private final ErstellerService erstellerService;

    private final Grid<Frage> grid = new Grid<>(Frage.class, false);
    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::aktualisiere);
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");

    private final TextArea frageFeld = new TextArea("Frage");
    private final TextField hintergrundFeld = new TextField("Hintergrund (optional)");
    private final TextField tagsFeld = new TextField("Schlagworte");
    private final ComboBox<String> zustaendigBox = new ComboBox<>("An (optional)");
    private Map<Long, Long> kontextAnzahl = Map.of();
    private final List<String> agenten = new ArrayList<>();

    public FragenView(FrageService frageService, ApiKeyRepository apiKeyRepository,
                      ErstellerService erstellerService, CurrentUser currentUser) {
        this.frageService = frageService;
        this.apiKeyRepository = apiKeyRepository;
        this.erstellerService = erstellerService;
        boolean kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);

        setSizeFull();

        frageFeld.setWidthFull();
        frageFeld.setPlaceholder("Was sollen die Agenten recherchieren? Am besten als prüfbare These oder klare Frage.");
        hintergrundFeld.setWidth("28em");
        hintergrundFeld.setPlaceholder("Wozu, worauf kommt es an?");
        tagsFeld.setWidth("16em");
        tagsFeld.setPlaceholder("kommasepariert, Hierarchie mit /");
        zustaendigBox.setWidth("16em");
        zustaendigBox.setPlaceholder("jeder Agent");
        zustaendigBox.setClearButtonVisible(true);
        zustaendigBox.setAllowCustomValue(true);
        zustaendigBox.addCustomValueSetListener(e -> {
            agenten.add(e.getDetail());
            zustaendigBox.setItems(agenten);
            zustaendigBox.setValue(e.getDetail());
        });

        Button stellen = new Button("Frage stellen", e -> stelleFrage());
        stellen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout zeile = new HorizontalLayout(hintergrundFeld, tagsFeld, zustaendigBox, stellen);
        zeile.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        zeile.setWidthFull();
        frageFeld.setVisible(kannBearbeiten);
        zeile.setVisible(kannBearbeiten);

        grid.addComponentColumn(f -> new RouterLink(f.toString(), FrageDetailView.class, f.getId()))
                .setHeader("Frage").setFlexGrow(1).setTooltipGenerator(Frage::getText);
        grid.addColumn(f -> f.getStatus().getAnzeigeName()).setHeader("Status").setAutoWidth(true).setSortable(true);
        grid.addColumn(f -> f.getZustaendig() != null ? f.getZustaendig() : "jeder").setHeader("An").setAutoWidth(true);
        grid.addColumn(f -> f.getBearbeitetVon() != null ? f.getBearbeitetVon()
                        : f.getBeantwortetVon() != null ? f.getBeantwortetVon() : "–")
                .setHeader("Bearbeitet von").setAutoWidth(true);
        grid.addColumn(f -> kontextAnzahl.getOrDefault(f.getId(), 0L)).setHeader("Kontext").setAutoWidth(true)
                .setTooltipGenerator(f -> "verknüpfte Aussagen im Netz");
        grid.addColumn(f -> f.getGestelltAm() != null ? f.getGestelltAm().format(QuelleDetailView.DATUM) : "")
                .setHeader("Gestellt").setAutoWidth(true)
                .setComparator(f -> f.getGestelltAm() != null ? f.getGestelltAm() : java.time.LocalDateTime.MIN);
        grid.setSizeFull();

        List<String> filterWerte = new ArrayList<>(List.of(NICHT_ERLEDIGT, ALLE));
        for (FrageStatus s : FrageStatus.values()) {
            filterWerte.add(s.getAnzeigeName());
        }
        statusFilter.setItems(filterWerte);
        statusFilter.setValue(NICHT_ERLEDIGT);
        statusFilter.addValueChangeListener(e -> aktualisiere());

        add(fehlerBanner, frageFeld, zeile,
                new Paragraph("Agenten holen offene Fragen per API, suchen Quellen und verknüpfen deren Aussagen "
                        + "mit der Frage. Du schließt die Frage ab oder hakst nach."),
                statusFilter, grid);
        aktualisiere();
    }

    private void stelleFrage() {
        if (frageFeld.isEmpty()) {
            Notification.show("Bitte eine Frage eingeben");
            return;
        }
        try {
            Frage f = frageService.stellen(frageFeld.getValue(), hintergrundFeld.getValue(), tagsFeld.getValue(),
                    zustaendigBox.getValue(), erstellerService.aktuellerErsteller());
            frageFeld.clear(); hintergrundFeld.clear(); tagsFeld.clear(); zustaendigBox.clear();
            aktualisiere();
            Notification.show("Frage gestellt (Nr. " + f.getId() + ")");
        } catch (RuntimeException e) {
            log.error("Speichern fehlgeschlagen", e);
            Notification n = Notification.show("Speichern fehlgeschlagen – ist die Datenbank erreichbar?",
                    5000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void aktualisiere() {
        try {
            String filter = statusFilter.getValue();
            List<Frage> fragen;
            if (filter == null || ALLE.equals(filter)) {
                fragen = frageService.alle(null);
            } else if (NICHT_ERLEDIGT.equals(filter)) {
                fragen = frageService.alle(null).stream().filter(f -> f.getStatus() != FrageStatus.ERLEDIGT).toList();
            } else {
                FrageStatus status = null;
                for (FrageStatus s : FrageStatus.values()) {
                    if (s.getAnzeigeName().equals(filter)) {
                        status = s;
                    }
                }
                fragen = frageService.alle(status);
            }
            kontextAnzahl = frageService.kontextAnzahl();
            grid.setItems(fragen);
            agenten.clear();
            apiKeyRepository.findAll().stream().filter(ApiKey::isAktiv).map(ApiKey::getName).sorted()
                    .forEach(agenten::add);
            zustaendigBox.setItems(agenten);
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            grid.setItems(List.of());
            fehlerBanner.setVisible(true);
        }
    }
}
