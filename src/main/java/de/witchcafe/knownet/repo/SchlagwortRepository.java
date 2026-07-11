package de.witchcafe.knownet.repo;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import de.witchcafe.knownet.domain.Schlagwort;

public interface SchlagwortRepository extends Neo4jRepository<Schlagwort, Long> {

    Optional<Schlagwort> findByName(String name);
}
