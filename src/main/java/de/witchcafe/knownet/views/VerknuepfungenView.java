package de.witchcafe.knownet.views;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.BeziehungsArt;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.service.Beziehung;
import de.witchcafe.knownet.service.BeziehungService;

@Route(value = "verknuepfungen", layout = MainLayout.class)
@PageTitle("Verknüpfungen | Knownet")
@AnonymousAllowed
public class VerknuepfungenView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(VerknuepfungenView.class);

    private final AussageRepository aussageRepository;
    private final BeziehungService beziehungService;
    private final boolean kannBearbeiten;

    private final Grid<Beziehung> grid = new Grid<>(Beziehung.class, false);
    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::aktualisiere);

    private final ComboBox<Aussage> vonBox = new ComboBox<>("Aussage");
    private final ComboBox<BeziehungsArt> artBox = new ComboBox<>("Beziehung");
    private final ComboBox<Aussage> zuBox = new ComboBox<>("Aussage");
    private final TextField kommentarFeld = new TextField("Kommentar (optional)");

    public VerknuepfungenView(AussageRepository aussageRepository,
                               BeziehungService beziehungService,
                               CurrentUser currentUser) {
        this.aussageRepository = aussageRepository;
        this.beziehungService = beziehungService;
        this.kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);

        setSizeFull();

        vonBox.setWidth("22em");
        vonBox.setItemLabelGenerator(Aussage::toString);
        zuBox.setWidth("22em");
        zuBox.setItemLabelGenerator(Aussage::toString);
        artBox.setWidth("12em");
        artBox.setItems(BeziehungsArt.values());
        artBox.setItemLabelGenerator(BeziehungsArt::getAnzeigeName);

        Button verknuepfen = new Button("Verknüpfen", e -> verknuepfe());
        verknuepfen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formular = new HorizontalLayout(vonBox, artBox, zuBox, kommentarFeld, verknuepfen);
        formular.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        formular.setWidthFull();
        formular.setVisible(kannBearbeiten);

        grid.addColumn(Beziehung::getVonText).setHeader("Von Aussage").setFlexGrow(2);
        grid.addColumn(Beziehung::getArt).setHeader("Beziehung").setAutoWidth(true);
        grid.addColumn(Beziehung::getZuText).setHeader("Zu Aussage").setFlexGrow(2);
        grid.addColumn(Beziehung::getKommentar).setHeader("Kommentar").setFlexGrow(1);

        if (kannBearbeiten) {
            grid.addComponentColumn(b -> {
                Button bearbeiten = new Button("Bearbeiten", e -> oeffneBearbeitenDialog(b));
                bearbeiten.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button loeschen = new Button("Löschen", e -> bestaetigenUndLoeschen(b));
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

    private void oeffneBearbeitenDialog(Beziehung beziehung) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Verknüpfung bearbeiten");
        dialog.setWidth("40em");

        TextField vonAnzeige = new TextField("Von Aussage");
        vonAnzeige.setValue(beziehung.getVonText());
        vonAnzeige.setReadOnly(true);
        vonAnzeige.setWidthFull();
        TextField artAnzeige = new TextField("Beziehung");
        artAnzeige.setValue(beziehung.getArt());
        artAnzeige.setReadOnly(true);
        artAnzeige.setWidthFull();
        TextField zuAnzeige = new TextField("Zu Aussage");
        zuAnzeige.setValue(beziehung.getZuText());
        zuAnzeige.setReadOnly(true);
        zuAnzeige.setWidthFull();
        TextField kommentarEdit = new TextField("Kommentar");
        kommentarEdit.setWidthFull();
        kommentarEdit.setValue(beziehung.getKommentar() != null ? beziehung.getKommentar() : "");

        FormLayout form = new FormLayout(vonAnzeige, artAnzeige, zuAnzeige, kommentarEdit);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button speichern = new Button("Speichern", e -> {
            try {
                beziehungService.aktualisiereKommentar(beziehung.getId(), kommentarEdit.getValue().trim());
                dialog.close();
                aktualisiere();
                Notification.show("Kommentar gespeichert");
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

    private void bestaetigenUndLoeschen(Beziehung beziehung) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Verknüpfung löschen?");
        confirm.setText("\"" + beziehung.getVonText() + " → " + beziehung.getArt()
                + " → " + beziehung.getZuText() + "\" wird gelöscht.");
        confirm.setCancelable(true);
        confirm.setCancelText("Abbrechen");
        confirm.setConfirmText("Löschen");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                beziehungService.loesche(beziehung.getId());
                aktualisiere();
                Notification.show("Verknüpfung gelöscht");
            } catch (RuntimeException ex) {
                log.error("Löschen fehlgeschlagen", ex);
                zeigeFehler("Löschen fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        confirm.open();
    }

    private void verknuepfe() {
        if (vonBox.getValue() == null || artBox.getValue() == null || zuBox.getValue() == null) {
            Notification.show("Bitte beide Aussagen und eine Beziehungsart wählen");
            return;
        }
        if (vonBox.getValue().getId().equals(zuBox.getValue().getId())) {
            Notification.show("Eine Aussage kann nicht mit sich selbst verknüpft werden");
            return;
        }
        try {
            beziehungService.verknuepfe(vonBox.getValue().getId(), zuBox.getValue().getId(),
                    artBox.getValue(), kommentarFeld.getValue());
            kommentarFeld.clear();
            aktualisiere();
            Notification.show("Verknüpfung angelegt");
        } catch (RuntimeException e) {
            log.error("Verknüpfen fehlgeschlagen", e);
            zeigeFehler("Verknüpfen fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private void aktualisiere() {
        try {
            grid.setItems(beziehungService.alleBeziehungen());
            List<Aussage> aussagen = aussageRepository.findAll();
            vonBox.setItems(aussagen);
            zuBox.setItems(aussagen);
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            grid.setItems(List.of());
            vonBox.setItems(List.of());
            zuBox.setItems(List.of());
            fehlerBanner.setVisible(true);
        }
    }

    private void zeigeFehler(String text) {
        Notification n = Notification.show(text, 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
