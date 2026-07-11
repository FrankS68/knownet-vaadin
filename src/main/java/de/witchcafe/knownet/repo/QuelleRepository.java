package de.witchcafe.knownet.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import de.witchcafe.knownet.domain.Quelle;

public interface QuelleRepository extends Neo4jRepository<Quelle, Long> {
}
