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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.SchlagwortService;

@Route(value = "aussagen", layout = MainLayout.class)
@PageTitle("Aussagen | Knownet")
@AnonymousAllowed
public class AussagenView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AussagenView.class);

    private final AussageRepository aussageRepository;
    private final QuelleRepository quelleRepository;
    private final SchlagwortService schlagwortService;
    private final boolean kannBearbeiten;

    private final Grid<Aussage> grid = new Grid<>(Aussage.class, false);
    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::aktualisiere);

    private final TextArea textFeld = new TextArea("Aussage");
    private final ComboBox<Quelle> quelleBox = new ComboBox<>("Quelle (optional)");
    private final TextField zitatFeld = new TextField("Originalzitat (optional)");
    private final TextField fundstelleFeld = new TextField("Fundstelle");
    private final TextField tagsFeld = new TextField("Schlagworte");

    public AussagenView(AussageRepository aussageRepository,
                        QuelleRepository quelleRepository,
                        SchlagwortService schlagwortService,
                        CurrentUser currentUser) {
        this.aussageRepository = aussageRepository;
        this.quelleRepository = quelleRepository;
        this.schlagwortService = schlagwortService;
        this.kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);

        setSizeFull();

        textFeld.setWidthFull();
        textFeld.setPlaceholder("Kernaussage in eigenen Worten ...");
        quelleBox.setWidth("20em");
        quelleBox.setItemLabelGenerator(Quelle::toString);
        quelleBox.setClearButtonVisible(true);
        zitatFeld.setWidth("24em");
        fundstelleFeld.setWidth("12em");
        fundstelleFeld.setPlaceholder("S. 12 / 03:41 / Kap. 2");
        tagsFeld.setWidth("16em");
        tagsFeld.setPlaceholder("kommasepariert");

        Button speichern = new Button("Aussage hinzufügen", e -> speichereAussage());
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout zeile = new HorizontalLayout(quelleBox, zitatFeld, fundstelleFeld, tagsFeld, speichern);
        zeile.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        zeile.setWidthFull();

        // Erfassungsbereich nur für Autoren/Admins
        textFeld.setVisible(kannBearbeiten);
        zeile.setVisible(kannBearbeiten);

        grid.addColumn(Aussage::getText).setHeader("Aussage").setFlexGrow(1);
        grid.addColumn(Aussage::getQuellenAlsText).setHeader("Quellen").setAutoWidth(true);
        grid.addColumn(Aussage::getSchlagworteAlsText).setHeader("Schlagworte").setAutoWidth(true);

        if (kannBearbeiten) {
            grid.addComponentColumn(aussage -> {
                Button bearbeiten = new Button("Bearbeiten", e -> oeffneBearbeitenDialog(aussage));
                bearbeiten.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button loeschen = new Button("Löschen", e -> bestaetigenUndLoeschen(aussage));
                loeschen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                HorizontalLayout aktionen = new HorizontalLayout(bearbeiten, loeschen);
                aktionen.setSpacing(true);
                return aktionen;
            }).setHeader("Aktionen").setAutoWidth(true);
        }

        grid.setSizeFull();
        add(fehlerBanner, textFeld, zeile, grid);
        aktualisiere();
    }

    private void oeffneBearbeitenDialog(Aussage aussage) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Aussage bearbeiten");
        dialog.setWidth("44em");

        TextArea textEdit = new TextArea("Aussage");
        textEdit.setWidthFull();
        textEdit.setMinHeight("6em");
        textEdit.setValue(aussage.getText() != null ? aussage.getText() : "");
        TextField tagsEdit = new TextField("Schlagworte (kommasepariert)");
        tagsEdit.setWidthFull();
        tagsEdit.setValue(aussage.getSchlagworteAlsText());

        FormLayout form = new FormLayout(textEdit, tagsEdit);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button speichern = new Button("Speichern", e -> {
            try {
                aussage.setText(textEdit.getValue().trim());
                aussage.setSchlagworte(schlagwortService.ausKommaListe(tagsEdit.getValue()));
                aussageRepository.save(aussage);
                dialog.close();
                aktualisiere();
                Notification.show("Aussage gespeichert");
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

    private void bestaetigenUndLoeschen(Aussage aussage) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Aussage löschen?");
        confirm.setText("Die Aussage und alle ihre Verknüpfungen werden unwiderruflich gelöscht.");
        confirm.setCancelable(true);
        confirm.setCancelText("Abbrechen");
        confirm.setConfirmText("Löschen");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                aussageRepository.deleteById(aussage.getId());
                aktualisiere();
                Notification.show("Aussage gelöscht");
            } catch (RuntimeException ex) {
                log.error("Löschen fehlgeschlagen", ex);
                zeigeFehler("Löschen fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        confirm.open();
    }

    private void speichereAussage() {
        if (textFeld.isEmpty()) { Notification.show("Bitte einen Aussagetext eingeben"); return; }
        try {
            Aussage aussage = new Aussage(textFeld.getValue().trim());
            aussage.setSchlagworte(schlagwortService.ausKommaListe(tagsFeld.getValue()));
            Quelle quelle = quelleBox.getValue();
            if (quelle != null) {
                aussage.getQuellen().add(new StammtAus(quelle,
                        zitatFeld.getValue().trim(), fundstelleFeld.getValue().trim()));
            }
            aussageRepository.save(aussage);
            textFeld.clear(); quelleBox.clear(); zitatFeld.clear();
            fundstelleFeld.clear(); tagsFeld.clear();
            aktualisiere();
            Notification.show("Aussage gespeichert");
        } catch (RuntimeException e) {
            log.error("Speichern fehlgeschlagen", e);
            zeigeFehler("Speichern fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private void aktualisiere() {
        try {
            grid.setItems(aussageRepository.findAll());
            quelleBox.setItems(quelleRepository.findAll());
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            grid.setItems(List.of());
            quelleBox.setItems(List.of());
            fehlerBanner.setVisible(true);
        }
    }

    private void zeigeFehler(String text) {
        Notification n = Notification.show(text, 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
