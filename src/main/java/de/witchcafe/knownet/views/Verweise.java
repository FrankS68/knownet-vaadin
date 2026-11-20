package de.witchcafe.knownet.views;

import java.util.Collection;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;

import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.service.ZusammenhangService.AussageBeziehung;

/**
 * Bausteine fuer Links zwischen den Detailseiten von Quellen, Aussagen und Schlagworten.
 */
final class Verweise {

    private Verweise() {}

    static RouterLink quelle(Quelle q) {
        return new RouterLink(q.toString(), QuelleDetailView.class, q.getId());
    }

    static RouterLink aussage(Long id, String text) {
        return new RouterLink(kuerze(text, 160), AussageDetailView.class, id);
    }

    static RouterLink schlagwort(String name) {
        RouterLink link = new RouterLink(name, SchlagwortDetailView.class, name);
        link.getStyle()
                .set("display", "inline-block")
                .set("padding", "0 0.5em")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("line-height", "1.8");
        return link;
    }

    /** Schlagwort-Chips nebeneinander; "–" wenn leer. */
    static Component schlagworte(Collection<String> namen) {
        Div box = new Div();
        box.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "0.3em");
        if (namen.isEmpty()) {
            box.add(grau("–"));
        } else {
            namen.stream().sorted().forEach(n -> box.add(schlagwort(n)));
        }
        return box;
    }

    /** Externer Link (neuer Tab), oder null wenn keine URL. */
    static Anchor extern(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Anchor a = new Anchor(url, url);
        a.setTarget("_blank");
        a.getStyle().set("word-break", "break-all");
        return a;
    }

    /** Eine Zeile "Bezeichnung: Inhalt". */
    static Div feld(String bezeichnung, Component... inhalt) {
        Div zeile = new Div();
        zeile.getStyle().set("display", "flex").set("gap", "0.5em").set("align-items", "baseline")
                .set("flex-wrap", "wrap");
        Span label = grau(bezeichnung);
        label.getStyle().set("min-width", "9em");
        zeile.add(label);
        for (Component c : inhalt) {
            if (c != null) {
                zeile.add(c);
            }
        }
        return zeile;
    }

    /** Listeneintrag mit Trennlinie. */
    static Div eintrag(Component... teile) {
        Div d = new Div();
        d.getStyle().set("padding", "0.5em 0").set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("width", "100%");
        for (Component c : teile) {
            if (c != null) {
                d.add(c);
            }
        }
        return d;
    }

    /** Originalzitat und Fundstelle, oder null wenn beides leer. */
    static Div zitat(String zitat, String fundstelle) {
        boolean hatZitat = zitat != null && !zitat.isBlank();
        boolean hatFund = fundstelle != null && !fundstelle.isBlank();
        if (!hatZitat && !hatFund) {
            return null;
        }
        Div d = new Div();
        d.getStyle().set("margin-left", "1em").set("font-size", "var(--lumo-font-size-s)");
        if (hatZitat) {
            Span z = new Span("„" + zitat + "“");
            z.getStyle().set("font-style", "italic");
            d.add(z);
        }
        if (hatFund) {
            d.add(grau((hatZitat ? " – " : "") + fundstelle));
        }
        return d;
    }

    /** Beziehungen einer Aussage, je eine Zeile; leer wenn keine. */
    static List<Component> beziehungen(List<AussageBeziehung> liste, String selbst) {
        return liste.stream().map(b -> {
            Div d = new Div();
            d.getStyle().set("margin-left", "1em").set("font-size", "var(--lumo-font-size-s)");
            Span art = new Span(" " + b.art() + " ");
            art.getStyle().set("font-weight", "600");
            if (b.ausgehend()) {
                d.add(grau(selbst), art, new Span("→ "), aussage(b.andereId(), b.andererText()));
            } else {
                d.add(aussage(b.andereId(), b.andererText()), art, new Span("→ "), grau(selbst));
            }
            if (b.kommentar() != null && !b.kommentar().isBlank()) {
                d.add(grau(" (" + b.kommentar() + ")"));
            }
            return (Component) d;
        }).toList();
    }

    static Span grau(String text) {
        Span s = new Span(text);
        s.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return s;
    }

    static String kuerze(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }
}
