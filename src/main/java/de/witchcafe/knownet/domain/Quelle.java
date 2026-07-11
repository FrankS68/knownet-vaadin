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
public class Quelle {

    @Id
    @GeneratedValue
    private Long id;

    private String url;
    private String titel;
    private String autor;
    private LocalDateTime erfasstAm = LocalDateTime.now();

    @Relationship(type = "HAT_SCHLAGWORT")
    private Set<Schlagwort> schlagworte = new HashSet<>();

    public Quelle() {
    }

    public Quelle(String url, String titel, String autor) {
        this.url = url;
        this.titel = titel;
        this.autor = autor;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public LocalDateTime getErfasstAm() {
        return erfasstAm;
    }

    public void setErfasstAm(LocalDateTime erfasstAm) {
        this.erfasstAm = erfasstAm;
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

    @Override
    public String toString() {
        return titel != null && !titel.isBlank() ? titel : url;
    }
}
