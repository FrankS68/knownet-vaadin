package de.witchcafe.knownet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;

/**
 * Explizite Konfiguration damit JPA- und Neo4j-TransactionManager
 * koexistieren können. JPA-TM ist Primary (Standard-Name "transactionManager"),
 * Neo4j-TM wird explizit in @EnableNeo4jRepositories referenziert.
 */
@Configuration
@EnableNeo4jRepositories(
        basePackages = "de.witchcafe.knownet.repo",
        transactionManagerRef = "neo4jTransactionManager",
        neo4jTemplateRef = "neo4jTemplate"
)
public class Neo4jConfig {

    /**
     * JPA TransactionManager als Primary — wird von witch-auth (@Transactional)
     * und Spring Boot Auto-Config als Standard verwendet.
     */
    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }

    @Bean("neo4jTemplate")
    public Neo4jOperations neo4jTemplate(Neo4jClient neo4jClient,
            Neo4jMappingContext mappingContext) {
        return new Neo4jTemplate(neo4jClient, mappingContext);
    }
}
