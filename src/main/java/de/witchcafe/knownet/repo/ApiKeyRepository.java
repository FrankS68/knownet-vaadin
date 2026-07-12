package de.witchcafe.knownet.repo;

import de.witchcafe.knownet.domain.ApiKey;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ApiKeyRepository extends Neo4jRepository<ApiKey, Long> {
    Optional<ApiKey> findByTokenHashAndAktivTrue(String tokenHash);
}
