package de.witchcafe.knownet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
// "de.witchcafe" deckt Knownet, witch-auth (de.witchcafe.auth) und witch-ui (de.witchcafe.ui) ab
@ComponentScan(basePackages = "de.witchcafe")
@EnableVaadin("de.witchcafe")
// JPA nur fuer die witch-auth-Benutzerdaten; die Knownet-Daten liegen in Neo4j (siehe Neo4jConfig)
@EnableJpaRepositories(basePackages = "de.witchcafe.auth.domain")
@EntityScan(basePackages = "de.witchcafe.auth.domain")
public class KnownetApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(KnownetApplication.class, args);
    }
}
