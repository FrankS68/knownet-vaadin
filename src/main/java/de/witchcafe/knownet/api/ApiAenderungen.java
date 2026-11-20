package de.witchcafe.knownet.api;

import java.util.List;

/**
 * Aenderungsprotokoll der REST-API fuer Agenten.
 * <p>
 * <b>Bei jeder Aenderung an der API hier oben einen Eintrag ergaenzen</b> (neueste zuerst).
 * Die Version des obersten Eintrags ist die aktuelle API-Version; sie steht in
 * {@code GET /api/docs} und in der OpenAPI-Beschreibung. Agenten merken sich die Version
 * und lesen bei einer Aenderung dieses Protokoll.
 * <p>
 * Versionierung: MAJOR bei inkompatiblen Aenderungen ({@code inkompatibel = true}: Felder/Endpunkte
 * entfernt oder umbenannt, Pflichtfelder neu), MINOR bei Erweiterungen, die bestehende Aufrufe nicht brechen.
 */
public final class ApiAenderungen {

    private ApiAenderungen() {}

    public record Eintrag(String version, String datum, boolean inkompatibel, List<String> aenderungen) {}

    public static final List<Eintrag> PROTOKOLL = List.of(
            new Eintrag("1.3.0", "2026-11-20", false, List.of(
                    "Neu: GET /api/schlagwoerter - alle Schlagworte (Themen) mit 'oberbegriffe', 'unterbegriffe', "
                            + "'pfade' (vollstaendige Pfade ab der Wurzel, direkt als 'schlagworte' beim Import verwendbar), "
                            + "'quellen' und 'aussagen' (Anzahl direkt zugeordneter Knoten).")),
            new Eintrag("1.2.0", "2026-11-20", false, List.of(
                    "Neu: Recherchefragen unter /api/fragen (GET /api/fragen, GET /api/fragen/offen, GET /api/fragen/{id}, "
                            + "POST /api/fragen, POST /api/fragen/{id}/uebernehmen, POST /api/fragen/{id}/freigeben, "
                            + "PUT /api/fragen/{id}/antwort). Ablauf siehe /api/docs/anleitung.",
                    "Neu: Beziehungsarten BEANTWORTET und BETRIFFT (fuer Verknuepfungen zu Frage-Aussagen).",
                    "POST /api/import: Aussagen duerfen per 'quelleId' auf eine vorhandene Quelle verweisen, "
                            + "Verknuepfungen per 'vonId'/'zuId' auf vorhandene Aussagen (statt vonRef/zuRef).",
                    "POST /api/import: Ergebnis enthaelt zusaetzlich 'quelleIds' und 'aussageIds' (Ref -> ID).",
                    "GET /api/verknuepfungen: zusaetzliche Felder 'vonId' und 'zuId'.",
                    "Neu: API-Doku unter /api/docs (ohne API-Key lesbar).")),
            new Eintrag("1.1.0", "2026-11-16", false, List.of(
                    "Quellen: Felder 'erstelltVon' (Kennung des Anlegers, bei API-Keys 'API: <Key-Name>') und 'erfasstAm'.",
                    "Neu: PUT /api/quellen/{id}/ki-bewertung mit {wahrscheinlichkeit 0-100, begruendung}; "
                            + "Quellen haben dazu 'kiWahrscheinlichkeit', 'kiBegruendung', 'kiBewertetVon', 'kiBewertetAm'.",
                    "Schlagworte duerfen Pfade sein ('weltwirtschaft/europawirtschaft'); zugeordnet wird die letzte Stufe.")),
            new Eintrag("1.0.0", "2026-07-13", false, List.of(
                    "Ausgangsstand: /api/quellen, /api/aussagen, /api/verknuepfungen, /api/import, /api/graph.")));

    public static String aktuelleVersion() {
        return PROTOKOLL.get(0).version();
    }
}
