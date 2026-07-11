package de.witchcafe.knownet.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle toggle = new DrawerToggle();

        H1 titel = new H1("Knownet");
        titel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Quellen", QuellenView.class));
        nav.addItem(new SideNavItem("Aussagen", AussagenView.class));
        nav.addItem(new SideNavItem("Verknüpfungen", VerknuepfungenView.class));

        addToDrawer(nav);
        addToNavbar(toggle, titel);
    }
}
