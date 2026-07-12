package de.witchcafe.knownet.domain;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * API-Key fuer maschinellen Zugriff (z.B. durch KI-Agenten).
 * Der Token wird gehasht gespeichert (SHA-256).
 */
@Node
public class ApiKey {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String tokenHash;
    private String rolle;
    private LocalDateTime erstelltAm = LocalDateTime.now();
    private boolean aktiv = true;

    public ApiKey() {}

    public ApiKey(String name, String tokenHash, String rolle) {
        this.name = name;
        this.tokenHash = tokenHash;
        this.rolle = rolle;
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public void setName(String n) { this.name = n; }
    public String getTokenHash()  { return tokenHash; }
    public String getRolle()      { return rolle; }
    public void setRolle(String r){ this.rolle = r; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public boolean isAktiv()      { return aktiv; }
    public void setAktiv(boolean a){ this.aktiv = a; }
}
