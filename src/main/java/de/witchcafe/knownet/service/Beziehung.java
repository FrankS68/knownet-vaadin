package de.witchcafe.knownet.service;

/**
 * DTO fuer die Anzeige einer Beziehung zwischen zwei Aussagen.
 */
public class Beziehung {

    private final Long id;
    private final Long vonId;
    private final String vonText;
    private final String art;
    private final Long zuId;
    private final String zuText;
    private final String kommentar;

    public Beziehung(Long id, Long vonId, String vonText, String art, Long zuId, String zuText, String kommentar) {
        this.id = id;
        this.vonId = vonId;
        this.vonText = vonText;
        this.art = art;
        this.zuId = zuId;
        this.zuText = zuText;
        this.kommentar = kommentar;
    }

    public Long getId() {
        return id;
    }

    public Long getVonId() {
        return vonId;
    }

    public String getVonText() {
        return vonText;
    }

    public String getArt() {
        return art;
    }

    public Long getZuId() {
        return zuId;
    }

    public String getZuText() {
        return zuText;
    }

    public String getKommentar() {
        return kommentar;
    }
}
