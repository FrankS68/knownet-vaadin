package de.witchcafe.knownet.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
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
        setSpacing(true);

        add(new Paragraph(
                "JSON-Datei mit Quellen, Aussagen und Verknüpfungen hochladen. " +
                "Format: { \"quellen\": [...], \"aussagen\": [...], \"verknuepfungen\": [...] }"));

        Pre beispiel = new Pre("""
                {
                  "quellen": [
                    {"ref":"q1","url":"https://...","titel":"Titel","autor":"Autor","schlagworte":"tag1, tag2"}
                  ],
                  "aussagen": [
                    {"ref":"a1","text":"Kernaussage","schlagworte":"tag1","quelleRef":"q1","zitat":"...","fundstelle":"S. 1"}
                  ],
                  "verknuepfungen": [
                    {"vonRef":"a1","zuRef":"a2","art":"BESTAETIGT","kommentar":"optional"}
                  ]
                }""");
        beispiel.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "1em")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-size", "0.85em")
                .set("overflow", "auto");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(10 * 1024 * 1024);

        // Eigener Upload-Button
        Button uploadButton = new Button("JSON-Datei auswählen und importieren");
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new Span("oder hier ablegen"));

        upload.addSucceededListener(event -> {
            try {
                ImportDaten daten = objectMapper.readValue(buffer.getInputStream(), ImportDaten.class);
                ImportErgebnis ergebnis = importController.verarbeite(daten);

                String msg = String.format("%d Quellen, %d Aussagen, %d Verknüpfungen importiert.",
                        ergebnis.quellenAngelegt(),
                        ergebnis.aussagenAngelegt(),
                        ergebnis.verknuepfungenAngelegt());

                if (!ergebnis.fehler().isEmpty()) {
                    Notification n = Notification.show(msg + " ⚠️ Fehler: " +
                            String.join("; ", ergebnis.fehler()), 8000, Notification.Position.MIDDLE);
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

        upload.addFileRejectedListener(event -> {
            Notification n = Notification.show("Datei abgelehnt: " + event.getErrorMessage(),
                    5000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        upload.addFailedListener(event -> {
            Notification n = Notification.show("Upload fehlgeschlagen: " + event.getReason(),
                    5000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        add(beispiel, upload);
    }
}
