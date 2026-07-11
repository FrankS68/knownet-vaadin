package de.witchcafe.knownet.domain;

/**
 * Typisierte Beziehungen zwischen zwei Aussagen.
 * Der Enum-Name wird direkt als Neo4j-Relationship-Typ verwendet.
 * Die Whitelist ueber das Enum verhindert Cypher-Injection,
 * da der Typname nicht per Parameter uebergeben werden kann.
 */
public enum BeziehungsArt {

    BESTAETIGT("bestätigt"),
    WIDERSPRICHT("widerspricht"),
    WIDERLEGT("widerlegt"),
    ERWEITERT("erweitert"),
    PRAEZISIERT("präzisiert"),
    RELATIVIERT("relativiert"),
    GEFAEHRDET("gefährdet"),
    SETZT_VORAUS("setzt voraus"),
    FOLGT_AUS("folgt aus"),
    VERANSCHAULICHT("veranschaulicht"),
    ERSETZT("ersetzt");

    private final String anzeigeName;

    BeziehungsArt(String anzeigeName) {
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
