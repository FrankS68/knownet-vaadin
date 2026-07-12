package de.witchcafe.knownet;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.witchcafe.auth.oauth2.OAuthUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
@Order(1)
public class KnownetSecurityConfig {

    private final OAuthUserService oAuthUserService;
    private final ApiKeyFilter apiKeyFilter;

    public KnownetSecurityConfig(OAuthUserService oAuthUserService, ApiKeyFilter apiKeyFilter) {
        this.oAuthUserService = oAuthUserService;
        this.apiKeyFilter = apiKeyFilter;
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // API-Key Filter vor Standard-Auth
        http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        http.with(VaadinSecurityConfigurer.vaadin(), cfg ->
                cfg.loginView("login"));

        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login.html")
                .defaultSuccessUrl("/", true)
                .userInfoEndpoint(ui -> ui
                        .userService(oauth2UserService())
                        .oidcUserService(oidcUserService()))
                .successHandler(oAuthUserService));

        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher("/api/graph"),
                        new AntPathRequestMatcher("/api/graph/**"),
                        new AntPathRequestMatcher("/login.html"),
                        new AntPathRequestMatcher("/images/**"),
                        new AntPathRequestMatcher("/*.png"),
                        new AntPathRequestMatcher("/oauth2/**"),
                        new AntPathRequestMatcher("/login/oauth2/**"),
                        new AntPathRequestMatcher("/logout")
                ).permitAll()
                // API-Endpunkte: Authentifizierung via Bearer Token
                .requestMatchers(
                        new AntPathRequestMatcher("/api/quellen/**"),
                        new AntPathRequestMatcher("/api/aussagen/**"),
                        new AntPathRequestMatcher("/api/verknuepfungen/**"),
                        new AntPathRequestMatcher("/api/import/**")
                ).authenticated());

        return http.build();
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            return oAuthUserService.enrichOAuth2User(user,
                    request.getClientRegistration().getRegistrationId());
        };
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            return oAuthUserService.enrichOidcUser(user,
                    request.getClientRegistration().getRegistrationId());
        };
    }
}
