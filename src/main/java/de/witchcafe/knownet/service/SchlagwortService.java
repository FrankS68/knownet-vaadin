package de.witchcafe.knownet.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import de.witchcafe.knownet.domain.Schlagwort;
import de.witchcafe.knownet.repo.SchlagwortRepository;

@Service
public class SchlagwortService {

    private final SchlagwortRepository repository;

    public SchlagwortService(SchlagwortRepository repository) {
        this.repository = repository;
    }

    /**
     * Findet ein Schlagwort per Name oder legt es an.
     * Namen werden normalisiert (getrimmt, kleingeschrieben),
     * damit "Klima" und "klima " nicht zu zwei Knoten werden.
     */
    public Schlagwort findeOderErzeuge(String name) {
        String normalisiert = name.trim().toLowerCase();
        return repository.findByName(normalisiert)
                .orElseGet(() -> repository.save(new Schlagwort(normalisiert)));
    }

    /**
     * Wandelt eine kommaseparierte Eingabe ("klima, energie, politik")
     * in eine Menge von Schlagwort-Knoten um.
     */
    public Set<Schlagwort> ausKommaListe(String eingabe) {
        Set<Schlagwort> ergebnis = new HashSet<>();
        if (eingabe == null || eingabe.isBlank()) {
            return ergebnis;
        }
        for (String teil : eingabe.split(",")) {
            if (!teil.isBlank()) {
                ergebnis.add(findeOderErzeuge(teil));
            }
        }
        return ergebnis;
    }
}
