package de.witchcafe.knownet.domain;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Beziehung Aussage -[:STAMMT_AUS]-> Quelle mit Eigenschaften
 * wie Originalzitat und Fundstelle (Seite, Timestamp, Abschnitt).
 */
@RelationshipProperties
public class StammtAus {

    @RelationshipId
    private Long id;

    private String zitat;
    private String fundstelle;

    @TargetNode
    private Quelle quelle;

    public StammtAus() {
    }

    public StammtAus(Quelle quelle, String zitat, String fundstelle) {
        this.quelle = quelle;
        this.zitat = zitat;
        this.fundstelle = fundstelle;
    }

    public Long getId() {
        return id;
    }

    public String getZitat() {
        return zitat;
    }

    public void setZitat(String zitat) {
        this.zitat = zitat;
    }

    public String getFundstelle() {
        return fundstelle;
    }

    public void setFundstelle(String fundstelle) {
        this.fundstelle = fundstelle;
    }

    public Quelle getQuelle() {
        return quelle;
    }

    public void setQuelle(Quelle quelle) {
        this.quelle = quelle;
    }
}
