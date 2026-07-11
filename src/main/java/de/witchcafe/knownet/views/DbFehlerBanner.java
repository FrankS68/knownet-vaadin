package de.witchcafe.knownet.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Hinweis-Banner, das angezeigt wird, wenn die Neo4j-Datenbank
 * nicht erreichbar ist. Bietet einen Button zum erneuten Laden.
 */
public class DbFehlerBanner extends HorizontalLayout {

    public DbFehlerBanner(Runnable neuLadenAktion) {
        Span text = new Span("Keine Verbindung zur Neo4j-Datenbank. "
                + "Bitte prüfen, ob die Datenbank läuft (Neo4j Desktop: DBMS starten, oder: docker compose up -d).");

        Button neuLaden = new Button("Erneut versuchen", e -> neuLadenAktion.run());
        neuLaden.addThemeVariants(ButtonVariant.LUMO_SMALL);

        add(text, neuLaden);
        setDefaultVerticalComponentAlignment(Alignment.CENTER);
        setWidthFull();
        setPadding(true);
        getStyle()
                .set("background-color", "var(--lumo-error-color-10pct)")
                .set("color", "var(--lumo-error-text-color)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        setVisible(false);
    }
}
