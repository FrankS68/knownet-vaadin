package de.witchcafe.knownet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.witchcafe.knownet.domain.Aussage;
import de.witchcafe.knownet.domain.BeziehungsArt;
import de.witchcafe.knownet.domain.Quelle;
import de.witchcafe.knownet.domain.StammtAus;
import de.witchcafe.knownet.repo.AussageRepository;
import de.witchcafe.knownet.repo.QuelleRepository;
import de.witchcafe.knownet.service.BeziehungService;
import de.witchcafe.knownet.service.SchlagwortService;

/**
 * Legt beim ersten Start Beispieldaten zur Klimaerwärmung an,
 * sofern die Datenbank noch leer ist.
 * Wird NACH dem vollständigen Start der Anwendung ausgeführt
 * (ApplicationReadyEvent) damit Neo4j-Transaktionen bereit sind.
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final QuelleRepository quelleRepo;
    private final AussageRepository aussageRepo;
    private final SchlagwortService schlagwortService;
    private final BeziehungService beziehungService;

    public DataInitializer(QuelleRepository quelleRepo,
                           AussageRepository aussageRepo,
                           SchlagwortService schlagwortService,
                           BeziehungService beziehungService) {
        this.quelleRepo = quelleRepo;
        this.aussageRepo = aussageRepo;
        this.schlagwortService = schlagwortService;
        this.beziehungService = beziehungService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            if (quelleRepo.count() > 0) {
                log.info("Datenbank enthält bereits Daten – Initialisierung übersprungen.");
                return;
            }
        } catch (Exception e) {
            log.warn("Konnte Datenbankzustand nicht prüfen – Initialisierung übersprungen: {}", e.getMessage());
            return;
        }

        log.info("Lege Beispieldaten zur Klimaerwärmung an ...");

        // --- Quellen ---
        Quelle ipcc = quelleRepo.save(quelle(
                "https://www.ipcc.ch/report/sixth-assessment-report-cycle/",
                "IPCC – AR6 Synthesis Report (2023)", "IPCC",
                "klimawandel, ipcc, treibhausgase, erwärmung"));

        Quelle nasa = quelleRepo.save(quelle(
                "https://science.nasa.gov/earth/explore/earth-indicators/global-temperature/",
                "NASA – Global Temperature (Earth Indicator)", "NASA",
                "klimawandel, nasa, temperatur, rekord"));

        Quelle noaa = quelleRepo.save(quelle(
                "https://www.climate.gov/news-features/understanding-climate/climate-change-global-temperature",
                "NOAA Climate.gov – Climate Change: Global Temperature", "NOAA",
                "klimawandel, noaa, temperatur, erwärmung"));

        Quelle nasaRecord = quelleRepo.save(quelle(
                "https://www.nasa.gov/news-release/temperatures-rising-nasa-confirms-2024-warmest-year-on-record/",
                "NASA – Wärmstes Jahr 2024 bestätigt", "NASA",
                "klimawandel, nasa, 2024, rekord"));

        Quelle wri = quelleRepo.save(quelle(
                "https://www.wri.org/insights/2023-ipcc-ar6-synthesis-report-climate-change-findings",
                "World Resources Institute – Top Findings IPCC 2023", "WRI",
                "klimawandel, ipcc, kipppunkte, treibhausgase"));

        // --- Aussagen ---
        Aussage a1 = aussageRepo.save(aussage(
                "Menschliche Aktivitäten haben eindeutig die globale Erwärmung verursacht – " +
                "globale Oberflächentemperaturen liegen 1,1 °C über dem vorindustriellen Niveau (1850–1900).",
                ipcc, "Menschliche Aktivitäten, primär durch Treibhausgasemissionen, haben eindeutig die globale Erwärmung verursacht",
                "AR6 Synthesis Report 2023", "klimawandel, ursache, temperatur, vorindustriell"));

        Aussage a2 = aussageRepo.save(aussage(
                "Es ist wahrscheinlich, dass die Erwärmung im 21. Jahrhundert 1,5 °C überschreiten wird.",
                ipcc, "wahrscheinlich, dass die Erwärmung im 21. Jahrhundert 1,5 °C überschreiten wird",
                "AR6 Synthesis Report 2023", "klimawandel, 1komma5grad, prognose, ipcc"));

        Aussage a3 = aussageRepo.save(aussage(
                "Die 10 jüngsten Jahre sind die wärmsten seit Beginn der Aufzeichnungen. " +
                "2024 bleibt das heißeste Jahr; 2025 lag 1,19 °C über dem Durchschnitt von 1951–1980.",
                nasa, "", "NASA Earth Indicators", "temperatur, rekord, nasa, 2024"));

        Aussage a4 = aussageRepo.save(aussage(
                "2024 war das wärmste Jahr seit Beginn der globalen Aufzeichnungen 1850 – " +
                "mit deutlichem Abstand, 1,35 °C über dem vorindustriellen Durchschnitt.",
                noaa, "", "NOAA Climate.gov", "temperatur, rekord, noaa, 2024"));

        Aussage a5 = aussageRepo.save(aussage(
                "Die kombinierte Land- und Ozeantemperatur hat sich seit 1975 mit 0,20 °C pro Jahrzehnt erwärmt – " +
                "mehr als dreimal so schnell wie die durchschnittliche Rate seit 1850.",
                noaa, "", "NOAA Climate.gov", "temperaturanstieg, beschleunigung, noaa"));

        Aussage a6 = aussageRepo.save(aussage(
                "Der Erwärmungstrend der letzten Jahrzehnte wird durch wärmespeicherndes Kohlendioxid, " +
                "Methan und andere Treibhausgase verursacht.",
                nasaRecord, "", "NASA Pressemitteilung 2025", "treibhausgase, co2, methan, ursache"));

        Aussage a7 = aussageRepo.save(aussage(
                "Wir sind in nur 150 Jahren auf halbem Weg zu einer Erwärmung auf Pliozän-Niveau. (Gavin Schmidt, NASA GISS)",
                nasaRecord, "Wir sind in nur 150 Jahren auf halbem Weg zu einer Erwärmung auf Pliozän-Niveau.",
                "NASA Pressemitteilung 2025", "klimawandel, pliozän, nasa, schmidt"));

        Aussage a8 = aussageRepo.save(aussage(
                "Steigende globale Temperaturen erhöhen die Wahrscheinlichkeit, gefährliche Kipppunkte zu erreichen, " +
                "die selbstverstärkende Rückkopplungen auslösen können – etwa das Auftauen von Permafrost.",
                wri, "", "WRI IPCC AR6 Findings", "kipppunkte, permafrost, rückkopplung, risiko"));

        // --- Verknüpfungen ---
        beziehungService.verknuepfe(a3.getId(), a4.getId(), BeziehungsArt.BESTAETIGT,
                "Beide Institutionen bestätigen 2024 als wärmstes Jahr");
        beziehungService.verknuepfe(a4.getId(), a3.getId(), BeziehungsArt.BESTAETIGT,
                "Gegenseitige Bestätigung NASA/NOAA");
        beziehungService.verknuepfe(a1.getId(), a2.getId(), BeziehungsArt.SETZT_VORAUS,
                "Aktuelle Erwärmung ist Basis für die 1,5°C-Prognose");
        beziehungService.verknuepfe(a5.getId(), a3.getId(), BeziehungsArt.ERWEITERT,
                "Beschleunigung des Trends erklärt die Rekordwerte");
        beziehungService.verknuepfe(a6.getId(), a1.getId(), BeziehungsArt.BESTAETIGT,
                "NASA benennt konkret CO2/Methan als Ursache");
        beziehungService.verknuepfe(a7.getId(), a2.getId(), BeziehungsArt.VERANSCHAULICHT,
                "Historischer Vergleich macht die Dimension der Prognose greifbar");
        beziehungService.verknuepfe(a8.getId(), a2.getId(), BeziehungsArt.FOLGT_AUS,
                "Kipppunkte werden bei Überschreitung von 1,5°C wahrscheinlicher");
        beziehungService.verknuepfe(a8.getId(), a5.getId(), BeziehungsArt.GEFAEHRDET,
                "Permafrost-Rückkopplung könnte Erwärmungstrend weiter beschleunigen");

        log.info("Beispieldaten angelegt: 5 Quellen, 8 Aussagen, 8 Verknüpfungen.");
    }

    private Quelle quelle(String url, String titel, String autor, String tags) {
        Quelle q = new Quelle(url, titel, autor);
        q.setSchlagworte(schlagwortService.ausKommaListe(tags));
        return q;
    }

    private Aussage aussage(String text, Quelle quelle, String zitat, String fundstelle, String tags) {
        Aussage a = new Aussage(text);
        a.setSchlagworte(schlagwortService.ausKommaListe(tags));
        if (quelle != null) {
            a.getQuellen().add(new StammtAus(quelle, zitat, fundstelle));
        }
        return a;
    }
}
