package de.witchcafe.knownet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme("knownet")
public class KnownetApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(KnownetApplication.class, args);
    }
}
