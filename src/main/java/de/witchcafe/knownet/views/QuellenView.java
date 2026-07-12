package de.witchcafe.knownet.views;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.SchlagwortService;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Quellen | Knownet")
@AnonymousAllowed
public class QuellenView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(QuellenView.class);

    private final QuelleRepository quelleRepository;
    private final SchlagwortService schlagwortService;
    private final boolean kannBearbeiten;

    private final Grid<Quelle> grid = new Grid<>(Quelle.class, false);
    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::aktualisiere);

    private final TextField urlFeld = new TextField("URL");
    private final TextField titelFeld = new TextField("Titel");
    private final TextField autorFeld = new TextField("Autor");
    private final TextField tagsFeld = new TextField("Schlagworte (kommasepariert)");

    public QuellenView(QuelleRepository quelleRepository,
                       SchlagwortService schlagwortService,
                       CurrentUser currentUser) {
        this.quelleRepository = quelleRepository;
        this.schlagwortService = schlagwortService;
        this.kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);

        setSizeFull();

        urlFeld.setWidth("28em");
        urlFeld.setPlaceholder("https://...");
        titelFeld.setWidth("20em");
        tagsFeld.setWidth("20em");
        tagsFeld.setPlaceholder("klima, energie, politik");

        Button speichern = new Button("Quelle hinzufügen", e -> speichereQuelle());
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formular = new HorizontalLayout(urlFeld, titelFeld, autorFeld, tagsFeld, speichern);
        formular.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        formular.setWidthFull();
        formular.setVisible(kannBearbeiten);

        grid.addColumn(Quelle::getTitel).setHeader("Titel").setAutoWidth(true);
        grid.addComponentColumn(q -> {
            Anchor link = new Anchor(q.getUrl() != null ? q.getUrl() : "", q.getUrl());
            link.setTarget("_blank");
            return link;
        }).setHeader("URL").setFlexGrow(1);
        grid.addColumn(Quelle::getAutor).setHeader("Autor").setAutoWidth(true);
        grid.addColumn(Quelle::getSchlagworteAlsText).setHeader("Schlagworte").setAutoWidth(true);

        if (kannBearbeiten) {
            grid.addComponentColumn(quelle -> {
                Button bearbeiten = new Button("Bearbeiten", e -> oeffneBearbeitenDialog(quelle));
                bearbeiten.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button loeschen = new Button("Löschen", e -> bestaetigenUndLoeschen(quelle));
                loeschen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                HorizontalLayout aktionen = new HorizontalLayout(bearbeiten, loeschen);
                aktionen.setSpacing(true);
                return aktionen;
            }).setHeader("Aktionen").setAutoWidth(true);
        }

        grid.setSizeFull();
        add(fehlerBanner, formular, grid);
        aktualisiere();
    }

    private void oeffneBearbeitenDialog(Quelle quelle) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Quelle bearbeiten");
        dialog.setWidth("40em");

        TextField urlEdit = new TextField("URL");
        urlEdit.setWidthFull();
        urlEdit.setValue(quelle.getUrl() != null ? quelle.getUrl() : "");
        TextField titelEdit = new TextField("Titel");
        titelEdit.setWidthFull();
        titelEdit.setValue(quelle.getTitel() != null ? quelle.getTitel() : "");
        TextField autorEdit = new TextField("Autor");
        autorEdit.setWidthFull();
        autorEdit.setValue(quelle.getAutor() != null ? quelle.getAutor() : "");
        TextField tagsEdit = new TextField("Schlagworte (kommasepariert)");
        tagsEdit.setWidthFull();
        tagsEdit.setValue(quelle.getSchlagworteAlsText());

        FormLayout form = new FormLayout(urlEdit, titelEdit, autorEdit, tagsEdit);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button speichern = new Button("Speichern", e -> {
            try {
                quelle.setUrl(urlEdit.getValue().trim());
                quelle.setTitel(titelEdit.getValue().trim());
                quelle.setAutor(autorEdit.getValue().trim());
                quelle.setSchlagworte(schlagwortService.ausKommaListe(tagsEdit.getValue()));
                quelleRepository.save(quelle);
                dialog.close();
                aktualisiere();
                Notification.show("Quelle gespeichert");
            } catch (RuntimeException ex) {
                log.error("Speichern fehlgeschlagen", ex);
                zeigeFehler("Speichern fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button abbrechen = new Button("Abbrechen", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(abbrechen, speichern);
        dialog.open();
    }

    private void bestaetigenUndLoeschen(Quelle quelle) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Quelle löschen?");
        confirm.setText("\"" + quelle + "\" wird unwiderruflich gelöscht.");
        confirm.setCancelable(true);
        confirm.setCancelText("Abbrechen");
        confirm.setConfirmText("Löschen");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                quelleRepository.deleteById(quelle.getId());
                aktualisiere();
                Notification.show("Quelle gelöscht");
            } catch (RuntimeException ex) {
                log.error("Löschen fehlgeschlagen", ex);
                zeigeFehler("Löschen fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        confirm.open();
    }

    private void speichereQuelle() {
        if (urlFeld.isEmpty()) { Notification.show("Bitte eine URL angeben"); return; }
        try {
            Quelle quelle = new Quelle(urlFeld.getValue().trim(),
                    titelFeld.getValue().trim(), autorFeld.getValue().trim());
            quelle.setSchlagworte(schlagwortService.ausKommaListe(tagsFeld.getValue()));
            quelleRepository.save(quelle);
            urlFeld.clear(); titelFeld.clear(); autorFeld.clear(); tagsFeld.clear();
            aktualisiere();
            Notification.show("Quelle gespeichert");
        } catch (RuntimeException e) {
            log.error("Speichern fehlgeschlagen", e);
            zeigeFehler("Speichern fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private void aktualisiere() {
        try {
            grid.setItems(quelleRepository.findAll());
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            grid.setItems(List.of());
            fehlerBanner.setVisible(true);
        }
    }

    private void zeigeFehler(String text) {
        Notification n = Notification.show(text, 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
