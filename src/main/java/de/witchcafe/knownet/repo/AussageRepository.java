package de.witchcafe.knownet.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import de.witchcafe.knownet.domain.Aussage;

public interface AussageRepository extends Neo4jRepository<Aussage, Long> {
}
