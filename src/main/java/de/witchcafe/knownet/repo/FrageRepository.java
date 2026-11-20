package de.witchcafe.knownet.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import de.witchcafe.knownet.domain.Frage;

public interface FrageRepository extends Neo4jRepository<Frage, Long> {
}
