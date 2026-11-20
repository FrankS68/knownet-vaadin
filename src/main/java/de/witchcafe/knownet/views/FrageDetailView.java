package de.witchcafe.knownet.views;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.Frage;
import de.witchcafe.knownet.domain.FrageStatus;
import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.service.ErstellerService;
import de.witchcafe.knownet.service.FrageService;
import de.witchcafe.knownet.service.ZusammenhangService;
import de.witchcafe.knownet.service.ZusammenhangService.AussageBeziehung;

/** Eine Recherchefrage mit Antwort, Verlauf und ihrem Kontext im Netz (verknüpfte Aussagen samt Quellen). */
@Route("frage")
@AnonymousAllowed
public class FrageDetailView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private static final Logger log = LoggerFactory.getLogger(FrageDetailView.class);

    private final FrageService frageService;
    private final AussageRepository aussageRepository;
    private final ZusammenhangService zusammenhang;
    private final ErstellerService erstellerService;
    private final boolean kannBearbeiten;

    private final DbFehlerBanner fehlerBanner = new DbFehlerBanner(this::lade);
    private final VerticalLayout inhalt = new VerticalLayout();
    private Long id;
    private String titel = "Frage";

    public FrageDetailView(FrageService frageService, AussageRepository aussageRepository,
                           ZusammenhangService zusammenhang, ErstellerService erstellerService,
                           CurrentUser currentUser) {
        this.frageService = frageService;
        this.aussageRepository = aussageRepository;
        this.zusammenhang = zusammenhang;
        this.erstellerService = erstellerService;
        this.kannBearbeiten = ViewSecurity.kannBearbeiten(currentUser);
        inhalt.setPadding(false);
        inhalt.setSpacing(false);
        inhalt.setMaxWidth("60em");
        add(fehlerBanner, inhalt);
    }

    @Override
    public void setParameter(BeforeEvent event, Long id) {
        this.id = id;
        lade();
    }

    @Override
    public String getPageTitle() {
        return titel + " | Knownet";
    }

    private void lade() {
        inhalt.removeAll();
        try {
            Optional<Frage> frage = frageService.finde(id);
            if (frage.isEmpty()) {
                inhalt.add(new H2("Frage nicht gefunden"), new RouterLink("Zu allen Fragen", FragenView.class));
            } else {
                zeige(frage.get());
            }
            fehlerBanner.setVisible(false);
        } catch (RuntimeException e) {
            log.error("Laden fehlgeschlagen", e);
            fehlerBanner.setVisible(true);
        }
    }

    private void zeige(Frage f) {
        titel = "Frage: " + Verweise.kuerze(f.getText(), 60);
        Paragraph text = new Paragraph(f.getText());
        text.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0.3em 0 0.8em 0");
        inhalt.add(new RouterLink("← Alle Fragen", FragenView.class), new H2("Frage"), text);

        inhalt.add(Verweise.feld("Status", statusMarke(f.getStatus())));
        inhalt.add(Verweise.feld("An", new Span(f.getZustaendig() != null ? f.getZustaendig() : "jeder Agent")));
        inhalt.add(Verweise.feld("Gestellt", new Span(
                (f.getGestelltAm() != null ? f.getGestelltAm().format(QuelleDetailView.DATUM) : "–")
                + (f.getGestelltVon() != null ? " von " + f.getGestelltVon() : ""))));
        if (f.getStatus() == FrageStatus.IN_ARBEIT && f.getBearbeitetVon() != null) {
            inhalt.add(Verweise.feld("In Arbeit", new Span(f.getBearbeitetVon()
                    + (f.getInArbeitSeit() != null ? " seit " + f.getInArbeitSeit().format(QuelleDetailView.DATUM) : ""))));
        }
        if (f.getHintergrund() != null) {
            inhalt.add(Verweise.feld("Hintergrund", new Span(f.getHintergrund())));
        }
        if (f.getAussage() != null) {
            inhalt.add(Verweise.feld("Schlagworte", Verweise.schlagworte(
                    f.getAussage().getSchlagworte().stream().map(Schlagwort::getName).toList())));
            inhalt.add(Verweise.feld("Im Netz", new RouterLink("als Aussage ansehen",
                    AussageDetailView.class, f.getAussage().getId())));
        }
        if (f.getStatus() == FrageStatus.OFFEN && f.getNachfrage() != null) {
            inhalt.add(Verweise.feld("Offene Nachfrage", new Span(f.getNachfrage())));
        }
        if (kannBearbeiten) {
            inhalt.add(aktionen(f));
        }

        if (f.getAntwort() != null) {
            inhalt.add(new H3("Antwort"));
            Paragraph antwort = new Paragraph(f.getAntwort());
            antwort.getStyle().set("white-space", "pre-wrap").set("margin", "0");
            inhalt.add(antwort);
            if (f.getBeantwortetVon() != null || f.getBeantwortetAm() != null) {
                inhalt.add(Verweise.grau((f.getBeantwortetVon() != null ? f.getBeantwortetVon() : "")
                        + (f.getBeantwortetAm() != null ? ", " + f.getBeantwortetAm().format(QuelleDetailView.DATUM) : "")));
            }
        }

        zeigeKontext(f);

        if (!f.getVerlauf().isEmpty()) {
            inhalt.add(new H3("Verlauf"));
            f.getVerlauf().forEach(v -> {
                Paragraph p = new Paragraph(v);
                p.getStyle().set("white-space", "pre-wrap").set("font-size", "var(--lumo-font-size-s)");
                inhalt.add(Verweise.eintrag(p));
            });
        }
    }

    /** Verknüpfte Aussagen, gruppiert nach Beziehungsart, jeweils mit ihren Quellen. */
    private void zeigeKontext(Frage f) {
        List<AussageBeziehung> beziehungen = f.getAussage() != null
                ? zusammenhang.beziehungenVon(f.getAussage().getId()) : List.of();
        inhalt.add(new H3("Kontext im Netz (" + beziehungen.size() + ")"));
        if (beziehungen.isEmpty()) {
            inhalt.add(new Paragraph("Noch keine Aussagen mit dieser Frage verknüpft."));
            return;
        }
        Map<Long, Aussage> aussagen = aussageRepository
                .findAllById(beziehungen.stream().map(AussageBeziehung::andereId).distinct().toList())
                .stream().collect(Collectors.toMap(Aussage::getId, Function.identity()));
        Map<String, List<AussageBeziehung>> nachArt = beziehungen.stream()
                .collect(Collectors.groupingBy(AussageBeziehung::art, LinkedHashMap::new, Collectors.toList()));
        nachArt.forEach((art, liste) -> {
            inhalt.add(new H4(art + " (" + liste.size() + ")"));
            for (AussageBeziehung b : liste) {
                Div eintrag = Verweise.eintrag(Verweise.aussage(b.andereId(), b.andererText()));
                if (b.ausgehend()) {
                    eintrag.add(Verweise.grau(" (Richtung: Frage " + art + " diese Aussage)"));
                }
                Aussage a = aussagen.get(b.andereId());
                if (a != null) {
                    for (StammtAus s : a.getQuellen()) {
                        if (s.getQuelle() != null) {
                            Div quelle = new Div(Verweise.grau("Quelle: "), Verweise.quelle(s.getQuelle()));
                            quelle.getStyle().set("margin-left", "1em").set("font-size", "var(--lumo-font-size-s)");
                            eintrag.add(quelle);
                            Div zitat = Verweise.zitat(s.getZitat(), s.getFundstelle());
                            if (zitat != null) {
                                eintrag.add(zitat);
                            }
                        }
                    }
                }
                if (b.kommentar() != null && !b.kommentar().isBlank()) {
                    Div k = new Div(Verweise.grau(b.kommentar()));
                    k.getStyle().set("margin-left", "1em").set("font-size", "var(--lumo-font-size-s)");
                    eintrag.add(k);
                }
                inhalt.add(eintrag);
            }
        });
    }

    private HorizontalLayout aktionen(Frage f) {
        HorizontalLayout leiste = new HorizontalLayout();
        leiste.getStyle().set("margin", "0.8em 0");
        if (f.getStatus() == FrageStatus.BEANTWORTET) {
            Button erledigt = new Button("Erledigt", e -> aendere(() -> frageService.setzeStatus(f, FrageStatus.ERLEDIGT),
                    "Frage abgeschlossen"));
            erledigt.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            leiste.add(erledigt);
        }
        if (f.getStatus() == FrageStatus.BEANTWORTET || f.getStatus() == FrageStatus.ERLEDIGT) {
            leiste.add(new Button(f.getStatus() == FrageStatus.ERLEDIGT ? "Wieder öffnen …" : "Nachhaken …",
                    e -> oeffneNachfrageDialog(f)));
        }
        if (f.getStatus() == FrageStatus.IN_ARBEIT) {
            leiste.add(new Button("Freigeben", e -> aendere(
                    () -> frageService.freigeben(f, erstellerService.aktuellerErsteller(), "von Hand freigegeben"),
                    "Frage ist wieder offen")));
        }
        Button loeschen = new Button("Löschen", e -> bestaetigenUndLoeschen(f));
        loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);
        leiste.add(loeschen);
        return leiste;
    }

    private void oeffneNachfrageDialog(Frage f) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nachfrage");
        dialog.setWidth("40em");
        TextArea feld = new TextArea("Was fehlt noch oder soll genauer recherchiert werden?");
        feld.setWidthFull();
        feld.setMinHeight("6em");
        Button senden = new Button("Frage wieder öffnen", e -> {
            if (feld.isEmpty()) {
                Notification.show("Bitte eine Nachfrage eingeben");
                return;
            }
            dialog.close();
            aendere(() -> frageService.nachhaken(f, feld.getValue(), erstellerService.aktuellerErsteller()),
                    "Frage ist wieder offen");
        });
        senden.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(feld);
        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), senden);
        dialog.open();
    }

    private void bestaetigenUndLoeschen(Frage f) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Frage löschen?");
        confirm.setText("Die Frage und ihre Frage-Aussage samt Verknüpfungen werden unwiderruflich gelöscht. "
                + "Die Aussagen und Quellen der Agenten bleiben erhalten.");
        confirm.setCancelable(true);
        confirm.setCancelText("Abbrechen");
        confirm.setConfirmText("Löschen");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                frageService.loesche(f);
                Notification.show("Frage gelöscht");
                UI.getCurrent().navigate(FragenView.class);
            } catch (RuntimeException ex) {
                log.error("Löschen fehlgeschlagen", ex);
                zeigeFehler("Löschen fehlgeschlagen – ist die Datenbank erreichbar?");
            }
        });
        confirm.open();
    }

    private void aendere(Runnable aktion, String meldung) {
        try {
            aktion.run();
            Notification.show(meldung);
            lade();
        } catch (RuntimeException ex) {
            log.error("Ändern fehlgeschlagen", ex);
            zeigeFehler("Ändern fehlgeschlagen – ist die Datenbank erreichbar?");
        }
    }

    private static Span statusMarke(FrageStatus status) {
        Span s = new Span(status.getAnzeigeName());
        String farbe = switch (status) {
            case OFFEN -> "var(--lumo-primary-color-10pct)";
            case IN_ARBEIT -> "var(--lumo-warning-color-10pct, #fff3cd)";
            case BEANTWORTET -> "var(--lumo-success-color-10pct)";
            case ERLEDIGT -> "var(--lumo-contrast-10pct)";
        };
        s.getStyle().set("background", farbe).set("padding", "0 0.6em")
                .set("border-radius", "var(--lumo-border-radius-l)");
        return s;
    }

    private void zeigeFehler(String text) {
        Notification n = Notification.show(text, 5000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
