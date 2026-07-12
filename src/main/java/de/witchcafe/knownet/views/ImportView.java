package de.witchcafe.knownet.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.knownet.api.ImportApiController;
import de.witchcafe.knownet.api.ImportApiController.ImportDaten;
import de.witchcafe.knownet.api.ImportApiController.ImportErgebnis;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/import", layout = MainLayout.class)
@PageTitle("Import | Knownet")
@RolesAllowed(AppRoles.ADMIN)
public class ImportView extends VerticalLayout {

    public ImportView(ImportApiController importController, ObjectMapper objectMapper) {
        setSizeFull();
        setPadding(true);

        add(new Paragraph(
            "JSON-Datei hochladen mit Quellen, Aussagen und Verknüpfungen. " +
            "Format: { \"quellen\": [...], \"aussagen\": [...], \"verknuepfungen\": [...] }"));

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/json", ".json");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("JSON-Datei hier ablegen oder klicken"));

        upload.addSucceededListener(event -> {
            try {
                ImportDaten daten = objectMapper.readValue(buffer.getInputStream(), ImportDaten.class);
                ImportErgebnis ergebnis = importController.verarbeite(daten);

                String msg = String.format("%d Quellen, %d Aussagen, %d Verknüpfungen importiert.",
                        ergebnis.quellenAngelegt(),
                        ergebnis.aussagenAngelegt(),
                        ergebnis.verknuepfungenAngelegt());

                if (!ergebnis.fehler().isEmpty()) {
                    msg += " Fehler: " + String.join("; ", ergebnis.fehler());
                    Notification n = Notification.show(msg, 8000, Notification.Position.MIDDLE);
                    n.addThemeVariants(NotificationVariant.LUMO_WARNING);
                } else {
                    Notification n = Notification.show("✅ " + msg, 5000, Notification.Position.MIDDLE);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            } catch (Exception e) {
                Notification n = Notification.show("Fehler: " + e.getMessage(), 6000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        add(upload);
    }
}
