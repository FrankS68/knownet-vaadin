package de.witchcafe.knownet.service;

/**
 * DTO fuer die Anzeige einer Beziehung zwischen zwei Aussagen.
 */
public class Beziehung {

    private final Long id;
    private final String vonText;
    private final String art;
    private final String zuText;
    private final String kommentar;

    public Beziehung(Long id, String vonText, String art, String zuText, String kommentar) {
        this.id = id;
        this.vonText = vonText;
        this.art = art;
        this.zuText = zuText;
        this.kommentar = kommentar;
    }

    public Long getId() {
        return id;
    }

    public String getVonText() {
        return vonText;
    }

    public String getArt() {
        return art;
    }

    public String getZuText() {
        return zuText;
    }

    public String getKommentar() {
        return kommentar;
    }
}
