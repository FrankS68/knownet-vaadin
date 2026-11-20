package de.witchcafe.knownet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.witchcafe.knownet.api.ApiAenderungen;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Kopf der OpenAPI-Beschreibung unter /api/docs/openapi.json (die Endpunkte erzeugt springdoc aus den Controllern). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI knownetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Knownet API")
                        .version(ApiAenderungen.aktuelleVersion())
                        .description("Wissensnetz aus Quellen, Aussagen, Schlagworten und Recherchefragen. "
                                + "Anleitung fuer Agenten: /api/docs/anleitung, Aenderungen: /api/docs/aenderungen."))
                .components(new Components().addSecuritySchemes("apiKey",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .description("API-Key aus der Verwaltung (Admin → API-Keys)")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
