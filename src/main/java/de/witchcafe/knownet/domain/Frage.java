package de.witchcafe.knownet.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Eine Recherchefrage eines Benutzers an die Agenten.
 * <p>
 * Die Frage steckt zugleich als {@link Aussage} im Netz ({@code (:Frage)-[:ALS_AUSSAGE]->(:Aussage)}):
 * Agenten verknuepfen die Aussagen ihrer Quellen mit dieser Frage-Aussage
 * (bestaetigt, widerspricht, beantwortet, betrifft ...). So steht die Frage im Kontext
 * des vorhandenen Wissens und erscheint auch in Graph und Verknuepfungen.
 */
@Node
public class Frage {

    @Id
    @GeneratedValue
    private Long id;

    private String text;
    /** Optionaler Hintergrund: warum gefragt wird, worauf es ankommt. */
    private String hintergrund;

    private String gestelltVon;
    private LocalDateTime gestelltAm = LocalDateTime.now();
    /** Optionaler Agent (Name bzw. Anfang des API-Key-Namens, z.B. "Kalliope"); null = jeder Agent. */
    private String zustaendig;

    private FrageStatus status = FrageStatus.OFFEN;
    private String bearbeitetVon;
    private LocalDateTime inArbeitSeit;

    private String antwort;
    private String beantwortetVon;
    private LocalDateTime beantwortetAm;
    /** Letzte Nachfrage des Benutzers (gilt, solange die Frage danach wieder offen ist). */
    private String nachfrage;
    /** Chronik: Antworten und Nachfragen, je ein Eintrag "dd.MM.yyyy HH:mm · wer · Art: Text". */
    private List<String> verlauf = new ArrayList<>();

    @Relationship(type = "ALS_AUSSAGE")
    private Aussage aussage;

    public Frage() {
    }

    public Frage(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHintergrund() {
        return hintergrund;
    }

    public void setHintergrund(String hintergrund) {
        this.hintergrund = hintergrund;
    }

    public String getGestelltVon() {
        return gestelltVon;
    }

    public void setGestelltVon(String gestelltVon) {
        this.gestelltVon = gestelltVon;
    }

    public LocalDateTime getGestelltAm() {
        return gestelltAm;
    }

    public String getZustaendig() {
        return zustaendig;
    }

    public void setZustaendig(String zustaendig) {
        this.zustaendig = zustaendig;
    }

    public FrageStatus getStatus() {
        return status;
    }

    public void setStatus(FrageStatus status) {
        this.status = status;
    }

    public String getBearbeitetVon() {
        return bearbeitetVon;
    }

    public void setBearbeitetVon(String bearbeitetVon) {
        this.bearbeitetVon = bearbeitetVon;
    }

    public LocalDateTime getInArbeitSeit() {
        return inArbeitSeit;
    }

    public void setInArbeitSeit(LocalDateTime inArbeitSeit) {
        this.inArbeitSeit = inArbeitSeit;
    }

    public String getAntwort() {
        return antwort;
    }

    public void setAntwort(String antwort) {
        this.antwort = antwort;
    }

    public String getBeantwortetVon() {
        return beantwortetVon;
    }

    public void setBeantwortetVon(String beantwortetVon) {
        this.beantwortetVon = beantwortetVon;
    }

    public LocalDateTime getBeantwortetAm() {
        return beantwortetAm;
    }

    public void setBeantwortetAm(LocalDateTime beantwortetAm) {
        this.beantwortetAm = beantwortetAm;
    }

    public String getNachfrage() {
        return nachfrage;
    }

    public void setNachfrage(String nachfrage) {
        this.nachfrage = nachfrage;
    }

    public List<String> getVerlauf() {
        if (verlauf == null) {
            verlauf = new ArrayList<>();
        }
        return verlauf;
    }

    public void setVerlauf(List<String> verlauf) {
        this.verlauf = verlauf;
    }

    public Aussage getAussage() {
        return aussage;
    }

    public void setAussage(Aussage aussage) {
        this.aussage = aussage;
    }

    @Override
    public String toString() {
        if (text == null) {
            return "";
        }
        return text.length() > 80 ? text.substring(0, 77) + "..." : text;
    }
}
