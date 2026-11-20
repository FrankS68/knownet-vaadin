package de.witchcafe.knownet.domain;

/** Ablauf einer Recherchefrage: offen → in Arbeit → beantwortet → erledigt (Nachfrage setzt wieder auf offen). */
public enum FrageStatus {

    OFFEN("offen"),
    IN_ARBEIT("in Arbeit"),
    BEANTWORTET("beantwortet"),
    ERLEDIGT("erledigt");

    private final String anzeigeName;

    FrageStatus(String anzeigeName) {
        this.anzeigeName = anzeigeName;
    }

    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public String toString() {
        return anzeigeName;
    }
}
