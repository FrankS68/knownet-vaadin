package de.witchcafe.knownet.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.service.SchlagwortService;
import de.witchcafe.knownet.service.SchlagwortService.Eintrag;

/**
 * Schlagwort-Hierarchie als Baum. Autoren koennen Oberbegriffe zuordnen und loesen
 * sowie ganze Pfade ("weltwirtschaft/europawirtschaft/deutsche wirtschaft") anlegen.
 */
@Route("schlagworte")
@Menu(order = 2.5, icon = "vaadin:file-tree", title = "Schlagworte")
@PageTitle("Schlagworte | Knownet")
@AnonymousAllowed
public class SchlagworteView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(SchlagworteView.class);

    /**
     * Ein Knoten im Baum. Da ein Schlagwort mehrere Oberbegriffe haben kann,
     * erscheint es ggf. mehrfach; {@code oberbegriff} unterscheidet die Vorkommen
     * (TreeData verlangt eindeutige Eintraege).
     */
    private record Knoten(Eintrag eintrag, String oberbegriff, String pfad) {}

    private final SchlagwortService schlagwortService;
    private final boolean kannBearbeiten;

    private final TreeGrid<Knoten> tree = new TreeGrid<>();
    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::aktualisiere);
    private final TextField pfadFeld = new TextField("Neuer Pfad");

    public SchlagworteView(SchlagwortService schlagwortService, CurrentUser currentUser) {
        this.schlagwortService = schlagwortService;
        this.kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);

        setSizeFull();

        pfadFeld.setWidth("36em");
        pfadFeld.setPlaceholder("weltwirtschaft/europawirtschaft/deutsche wirtschaft/westerwald wirtschaft");
        Button anlegen = new Button("Anlegen", e -> legePfadAn());
        anlegen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout formular = new HorizontalLayout(pfadFeld, anlegen);
        formular.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        formular.setVisible(kannBearbeiten);

        tree.addHierarchyColumn(k -> k.eintrag().name()).setHeader("Schlagwort").setFlexGrow(1);
        tree.addColumn(k -> k.eintrag().quellen()).setHeader("Quellen").setAutoWidth(true);
        tree.addColumn(k -> k.eintrag().aussagen()).setHeader("Aussagen").setAutoWidth(true);
        tree.addColumn(k -> String.join(", ", k.eintrag().oberbegriffe())).setHeader("Oberbegriffe").setAutoWidth(true);

        if (kannBearbeiten) {
            tree.addComponentColumn(k -> {
                Button zuordnen = new Button("Oberbegriff …", e -> oeffneZuordnenDialog(k));
                zuordnen.addThemeVariants(ButtonVariant.LUMO_SMALL);
                HorizontalLayout aktionen = new HorizontalLayout(zuordnen);
                if (k.oberbegriff() != null) {
                    Button loesen = new Button("von „" + k.oberbegriff() + "“ lösen", e -> loese(k));
                    loesen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                    aktionen.add(loesen);
                }
                return aktionen;
            }).setHeader("Aktionen").setAutoWidth(true);
        }

        tree.setSizeFull();
        add(fehlerBanner,
                new Paragraph("Zählung: direkt zugeordnete Quellen/Aussagen. Der Filter in Quellen und Aussagen "
                        + "berücksichtigt auch alle Unterbegriffe."),
                formular, tree);
        aktualisiere();
    }

    private void aktualisiere() {
        try {
            List<Eintrag> eintraege = schlagwortService.alleEintraege();
            Map<String, List<Eintrag>> kinder = new LinkedHashMap<>();
            for (Eintrag e : eintraege) {
                for (String o : e.oberbegriffe()) {
                    kinder.computeIfAbsent(o, x -> new ArrayList<>()).add(e);
                }
            }

            TreeData<Knoten> daten = new TreeData<>();
            for (Eintrag e : eintraege) {
                if (e.oberbegriffe().isEmpty()) {
                    Knoten wurzel = new Knoten(e, null, e.name());
                    daten.addItem(null, wurzel);
                    fuegeKinderHinzu(daten, wurzel, kinder, new HashSet<>(Set.of(e.name())));
                }
            }
            tree.setTreeData(daten);
            tree.expandRecursively(daten.getRootItems(), Integer.MAX_VALUE);
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            tree.setTreeData(new TreeData<>());
            fehlerBanner.setVisible(true);
        }
    }

    private void fuegeKinderHinzu(TreeData<Knoten> daten, Knoten eltern,
                                  Map<String, List<Eintrag>> kinder, Set<String> aufPfad) {
        for (Eintrag kind : kinder.getOrDefault(eltern.eintrag().name(), List.of())) {
            if (!aufPfad.add(kind.name())) {
                continue; // Zyklusschutz, sollte durch den Service nie vorkommen
            }
            Knoten k = new Knoten(kind, eltern.eintrag().name(), eltern.pfad() + "/" + kind.name());
            daten.addItem(eltern, k);
            fuegeKinderHinzu(daten, k, kinder, aufPfad);
            aufPfad.remove(kind.name());
        }
    }

    private void legePfadAn() {
        if (pfadFeld.isEmpty()) {
            Notification.show("Bitte einen Pfad angeben");
            return;
        }
        try {
            schlagwortService.ausPfad(pfadFeld.getValue());
            pfadFeld.clear();
            aktualisiere();
            Notification.show("Schlagworte angelegt");
        } catch (RuntimeException ex) {
            log.error("Anlegen fehlgeschlagen", ex);
            zeigeFehler("Anlegen fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private void oeffneZuordnenDialog(Knoten k) {
        String name = k.eintrag().name();
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Oberbegriff für „" + name + "“");

        ComboBox<String> oberBox = new ComboBox<>("Oberbegriff (vorhanden oder neu)");
        oberBox.setWidth("24em");
        oberBox.setAllowCustomValue(true);
        List<String> auswahl = new ArrayList<>(schlagwortService.alleNamen().stream()
                .filter(n -> !n.equals(name) && !k.eintrag().oberbegriffe().contains(n))
                .toList());
        oberBox.setItems(auswahl);
        oberBox.addCustomValueSetListener(e -> {
            auswahl.add(e.getDetail());
            oberBox.setItems(auswahl);
            oberBox.setValue(e.getDetail());
        });

        Button speichern = new Button("Zuordnen", e -> {
            String ober = oberBox.getValue();
            if (ober == null || ober.isBlank()) {
                Notification.show("Bitte einen Oberbegriff wählen");
                return;
            }
            try {
                if (schlagwortService.setzeOberbegriff(name, ober)) {
                    dialog.close();
                    aktualisiere();
                    Notification.show("„" + name + "“ ist jetzt Unterbegriff von „" + ober.trim().toLowerCase() + "“");
                } else {
                    zeigeFehler("Nicht möglich: das würde einen Kreis in der Hierarchie erzeugen.");
                }
            } catch (RuntimeException ex) {
                log.error("Zuordnen fehlgeschlagen", ex);
                zeigeFehler("Zuordnen fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button abbrechen = new Button("Abbrechen", e -> dialog.close());
        dialog.add(oberBox);
        dialog.getFooter().add(abbrechen, speichern);
        dialog.open();
    }

    private void loese(Knoten k) {
        try {
            schlagwortService.entferneOberbegriff(k.eintrag().name(), k.oberbegriff());
            aktualisiere();
            Notification.show("Beziehung gelöst");
        } catch (RuntimeException ex) {
            log.error("Lösen fehlgeschlagen", ex);
            zeigeFehler("Lösen fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private void zeigeFehler(String text) {
        Notification n = Notification.show(text, 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
