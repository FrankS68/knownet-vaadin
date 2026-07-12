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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
@Order(1)
public class KnownetSecurityConfig {

    private final OAuthUserService oAuthUserService;

    public KnownetSecurityConfig(OAuthUserService oAuthUserService) {
        this.oAuthUserService = oAuthUserService;
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Vaadin Security — loginView steuert wohin unauthentifizierte Vaadin-Requests gehen
        http.with(VaadinSecurityConfigurer.vaadin(), cfg ->
                cfg.loginView("login"));

        // OAuth2 Login
        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login.html")
                .defaultSuccessUrl("/", true)
                .userInfoEndpoint(ui -> ui
                        .userService(oauth2UserService())
                        .oidcUserService(oidcUserService()))
                .successHandler(oAuthUserService));

        // Logout — explizit als Spring-Security-Endpunkt, nicht als Vaadin-Route
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login.html")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

        // Oeffentliche Endpunkte
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
                ).permitAll());

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
