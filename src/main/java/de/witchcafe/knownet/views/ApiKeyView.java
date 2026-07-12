package de.witchcafe.knownet.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.knownet.KnownetRoles;
import de.witchcafe.knownet.domain.ApiKey;
import de.witchcafe.knownet.service.ApiKeyService;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "admin/apikeys", layout = MainLayout.class)
@PageTitle("API-Keys | Knownet")
@RolesAllowed(AppRoles.ADMIN)
public class ApiKeyView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ApiKeyService apiKeyService;
    private final Grid<ApiKey> grid = new Grid<>(ApiKey.class, false);

    public ApiKeyView(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        setSizeFull();

        Button neuerKey = new Button("Neuer API-Key", e -> oeffneNeuDialog());
        neuerKey.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(ApiKey::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(ApiKey::getRolle).setHeader("Rolle").setAutoWidth(true);
        grid.addColumn(k -> FMT.format(k.getErstelltAm())).setHeader("Erstellt").setAutoWidth(true);
        grid.addComponentColumn(k -> {
            Span badge = new Span(k.isAktiv() ? "Aktiv" : "Deaktiviert");
            badge.getElement().getThemeList().add(k.isAktiv() ? "badge success" : "badge error");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addComponentColumn(k -> {
            HorizontalLayout aktionen = new HorizontalLayout();

            if (k.isAktiv()) {
                Button deaktivieren = new Button("Deaktivieren", e -> {
                    apiKeyService.deaktiviere(k.getId());
                    aktualisiere();
                    Notification.show("Key deaktiviert");
                });
                deaktivieren.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                aktionen.add(deaktivieren);
            }

            Button loeschen = new Button("Löschen", e -> {
                ConfirmDialog d = new ConfirmDialog();
                d.setHeader("API-Key löschen?");
                d.setText("\"" + k.getName() + "\" wird unwiderruflich gelöscht.");
                d.setCancelable(true);
                d.setCancelText("Abbrechen");
                d.setConfirmText("Löschen");
                d.setConfirmButtonTheme("error primary");
                d.addConfirmListener(ev -> {
                    apiKeyService.loesche(k.getId());
                    aktualisiere();
                    Notification.show("Key gelöscht");
                });
                d.open();
            });
            loeschen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            aktionen.add(loeschen);

            return aktionen;
        }).setHeader("Aktionen").setAutoWidth(true);

        grid.setSizeFull();
        add(neuerKey, grid);
        aktualisiere();
    }

    private void oeffneNeuDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neuer API-Key");

        TextField nameFeld = new TextField("Name (z.B. Claude-Agent)");
        nameFeld.setWidthFull();

        Select<String> rolleFeld = new Select<>();
        rolleFeld.setLabel("Rolle");
        rolleFeld.setItems(KnownetRoles.AUTOR, KnownetRoles.KOMMENTATOR, AppRoles.ADMIN);
        rolleFeld.setValue(KnownetRoles.AUTOR);
        rolleFeld.setWidthFull();

        FormLayout form = new FormLayout(nameFeld, rolleFeld);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button erzeugen = new Button("Erzeugen", e -> {
            if (nameFeld.isEmpty()) {
                Notification.show("Bitte einen Namen angeben");
                return;
            }
            String token = apiKeyService.erzeugeApiKey(nameFeld.getValue().trim(), rolleFeld.getValue());
            dialog.close();
            aktualisiere();
            zeigeToken(token, nameFeld.getValue().trim());
        });
        erzeugen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button abbrechen = new Button("Abbrechen", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(abbrechen, erzeugen);
        dialog.open();
    }

    private void zeigeToken(String token, String name) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("API-Key für \"" + name + "\"");
        dialog.setCloseOnOutsideClick(false);

        TextField tokenFeld = new TextField("Token — nur jetzt sichtbar, bitte sofort kopieren!");
        tokenFeld.setValue(token);
        tokenFeld.setWidthFull();
        tokenFeld.setReadOnly(true);

        Button kopieren = new Button("Kopieren", e -> {
            tokenFeld.getElement().executeJs(
                "navigator.clipboard.writeText($0)", token);
            Notification n = Notification.show("Token kopiert!", 2000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        kopieren.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button schliessen = new Button("Verstanden, schließen", e -> dialog.close());

        dialog.add(tokenFeld);
        dialog.getFooter().add(kopieren, schliessen);
        dialog.open();
    }

    private void aktualisiere() {
        grid.setItems(apiKeyService.alleKeys());
    }
}
