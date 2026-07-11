package de.witchcafe.knownet.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
public class Aussage {

    @Id
    @GeneratedValue
    private Long id;

    private String text;
    private LocalDateTime erstelltAm = LocalDateTime.now();

    @Relationship(type = "STAMMT_AUS")
    private Set<StammtAus> quellen = new HashSet<>();

    @Relationship(type = "HAT_SCHLAGWORT")
    private Set<Schlagwort> schlagworte = new HashSet<>();

    public Aussage() {
    }

    public Aussage(String text) {
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

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }

    public void setErstelltAm(LocalDateTime erstelltAm) {
        this.erstelltAm = erstelltAm;
    }

    public Set<StammtAus> getQuellen() {
        return quellen;
    }

    public void setQuellen(Set<StammtAus> quellen) {
        this.quellen = quellen;
    }

    public Set<Schlagwort> getSchlagworte() {
        return schlagworte;
    }

    public void setSchlagworte(Set<Schlagwort> schlagworte) {
        this.schlagworte = schlagworte;
    }

    public String getSchlagworteAlsText() {
        return schlagworte.stream()
                .map(Schlagwort::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public String getQuellenAlsText() {
        return quellen.stream()
                .map(s -> s.getQuelle().toString())
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        if (text == null) {
            return "";
        }
        return text.length() > 80 ? text.substring(0, 77) + "..." : text;
    }
}
