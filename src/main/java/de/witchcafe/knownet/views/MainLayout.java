package de.witchcafe.knownet.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.witchcafe.auth.CurrentUser;

public class MainLayout extends AppLayout {

    public MainLayout(CurrentUser currentUser) {
        DrawerToggle toggle = new DrawerToggle();

        H1 titel = new H1("Knownet");
        titel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Quellen", QuellenView.class));
        nav.addItem(new SideNavItem("Aussagen", AussagenView.class));
        nav.addItem(new SideNavItem("Verknüpfungen", VerknuepfungenView.class));
        nav.addItem(new SideNavItem("Graph", GraphView.class));

        if (ViewSecurity.istAdmin(currentUser)) {
            nav.addItem(new SideNavItem("Benutzer", BenutzerView.class));
            nav.addItem(new SideNavItem("API-Keys", ApiKeyView.class));
        }

        addToDrawer(nav);

        HorizontalLayout navBar = new HorizontalLayout();
        navBar.setWidthFull();
        navBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navBar.add(toggle, titel);
        navBar.expand(titel);

        currentUser.get().ifPresentOrElse(
            user -> {
                Avatar avatar = new Avatar(user.getFullName());
                if (user.getPictureUrl() != null) avatar.setImage(user.getPictureUrl());
                avatar.setTooltipEnabled(true);

                Span name = new Span(user.getFullName());
                name.addClassNames(LumoUtility.FontSize.SMALL);

                Anchor logout = new Anchor("/logout", "Abmelden");
                logout.addClassNames(LumoUtility.FontSize.SMALL);

                HorizontalLayout userArea = new HorizontalLayout(avatar, name, logout);
                userArea.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                userArea.setSpacing(true);
                navBar.add(userArea);
            },
            () -> {
                Button login = new Button("Anmelden");
                login.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                login.addClickListener(e ->
                    login.getUI().ifPresent(ui -> ui.navigate("login")));
                navBar.add(login);
            }
        );

        addToNavbar(navBar);
    }
}
