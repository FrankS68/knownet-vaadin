package de.witchcafe.knownet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme("knownet")
// JPA-Repos + Entities: witch-auth Benutzerverwaltung
@EnableJpaRepositories(basePackages = "de.witchcafe.auth.domain")
@EntityScan(basePackages = "de.witchcafe.auth.domain")
public class KnownetApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(KnownetApplication.class, args);
    }
}
