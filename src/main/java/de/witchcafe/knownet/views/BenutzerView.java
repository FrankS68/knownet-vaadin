package de.witchcafe.knownet.views;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.auth.domain.AppUser;
import de.witchcafe.auth.domain.UserManagementService;
import de.witchcafe.knownet.KnownetRoles;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/benutzer", layout = MainLayout.class)
@PageTitle("Benutzerverwaltung | Knownet")
@RolesAllowed(AppRoles.ADMIN)
public class BenutzerView extends VerticalLayout {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final UserManagementService service;
    private final Grid<AppUser> grid = new Grid<>(AppUser.class, false);

    public BenutzerView(UserManagementService service) {
        this.service = service;
        setSizeFull();

        grid.addComponentColumn(u -> {
            if (u.getPictureUrl() != null) {
                Image img = new Image(u.getPictureUrl(), u.getName());
                img.setWidth("32px");
                img.setHeight("32px");
                img.getStyle().set("border-radius", "50%");
                return img;
            }
            return new Span("👤");
        }).setWidth("50px").setFlexGrow(0);

        grid.addColumn(AppUser::getName).setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.addColumn(AppUser::getEmail).setHeader("E-Mail").setSortable(true).setAutoWidth(true);
        grid.addColumn(AppUser::getProvider).setHeader("Provider").setAutoWidth(true);

        grid.addComponentColumn(u -> {
            Span badge = new Span(u.getRole());
            badge.getElement().getThemeList().add(
                    AppRoles.ADMIN.equals(u.getRole()) ? "badge contrast" : "badge");
            return badge;
        }).setHeader("Rolle").setAutoWidth(true);

        grid.addComponentColumn(u -> {
            Span badge = new Span(u.isBlocked() ? "Geblockt" : "Aktiv");
            badge.getElement().getThemeList().add(
                    u.isBlocked() ? "badge error" : "badge success");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addColumn(u -> u.getLastLoginAt() != null ? FMT.format(u.getLastLoginAt()) : "-")
                .setHeader("Letzter Login").setAutoWidth(true);

        grid.addComponentColumn(this::aktionsButtons)
                .setHeader("Rolle setzen").setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(u -> {
            if (u.isBlocked()) {
                Button btn = new Button("Entsperren", e -> {
                    service.unblockUser(u.getUserId());
                    aktualisiere();
                    info(u.getName() + " entsperrt.");
                });
                btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                return btn;
            } else {
                Button btn = new Button("Blockieren", e -> bestaetigen(
                        "Benutzer blockieren?",
                        u.getName() + " kann sich dann nicht mehr anmelden.",
                        () -> { service.blockUser(u.getUserId()); aktualisiere(); fehler(u.getName() + " blockiert."); }
                ));
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                return btn;
            }
        }).setHeader("Sperren").setAutoWidth(true).setFlexGrow(0);

        grid.setSizeFull();
        add(grid);
        aktualisiere();
    }

    private HorizontalLayout aktionsButtons(AppUser u) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);

        for (String rolle : List.of(AppRoles.ADMIN, KnownetRoles.AUTOR, KnownetRoles.KOMMENTATOR, AppRoles.USER)) {
            if (!rolle.equals(u.getRole())) {
                Button btn = new Button("→ " + rolle, e -> bestaetigen(
                        "Rolle ändern?",
                        u.getName() + " bekommt die Rolle " + rolle + ".",
                        () -> { service.setRole(u.getUserId(), rolle); aktualisiere(); info(u.getName() + " → " + rolle); }
                ));
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL,
                        AppRoles.ADMIN.equals(rolle) ? ButtonVariant.LUMO_CONTRAST : ButtonVariant.LUMO_TERTIARY);
                layout.add(btn);
            }
        }
        return layout;
    }

    private void bestaetigen(String titel, String text, Runnable aktion) {
        ConfirmDialog d = new ConfirmDialog();
        d.setHeader(titel);
        d.setText(text);
        d.setCancelable(true);
        d.setCancelText("Abbrechen");
        d.setConfirmText("Bestätigen");
        d.addConfirmListener(e -> aktion.run());
        d.open();
    }

    private void aktualisiere() {
        grid.setItems(service.findAllUsers());
    }

    private void info(String msg) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void fehler(String msg) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
