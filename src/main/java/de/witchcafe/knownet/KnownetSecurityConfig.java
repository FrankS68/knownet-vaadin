package de.witchcafe.knownet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Knownet-spezifische Security fuer die REST-API.
 *
 * Login, Logout und die Vaadin-Oberflaeche sichert witch-auth ab
 * (de.witchcafe.auth.oauth2.OAuthSecurityConfig, Login-View unter /login).
 * Hier stehen nur eigene Filterketten mit securityMatcher und eigenen Bean-Namen,
 * damit sie die witch-auth-Kette nicht ueberschreiben.
 */
@Configuration
public class KnownetSecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    public KnownetSecurityConfig(ApiKeyFilter apiKeyFilter) {
        this.apiKeyFilter = apiKeyFilter;
    }

    /** Graph-API und API-Doku: oeffentlich lesbar (GraphView, externe Clients, Agenten ohne Key). */
    @Bean
    @Order(0)
    public SecurityFilterChain apiGraphFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/graph", "/api/graph/**", "/api/docs", "/api/docs/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /** Uebrige API: Authentifizierung per "Authorization: Bearer &lt;API-Key&gt;". */
    @Bean
    @Order(1)
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
