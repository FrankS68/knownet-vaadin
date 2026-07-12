package de.witchcafe.knownet;

import de.witchcafe.knownet.domain.ApiKey;
import de.witchcafe.knownet.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Prueft Authorization: Bearer <token> Header fuer API-Endpunkte.
 * Setzt bei gueltigem Token die entsprechende Rolle in den SecurityContext.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Nur fuer /api/** relevant
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            apiKeyService.findByToken(token).ifPresent(apiKey -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        apiKey.getName(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + apiKey.getRolle()),
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }
}
