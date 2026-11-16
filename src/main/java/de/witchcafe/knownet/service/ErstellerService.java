package de.witchcafe.knownet.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import de.witchcafe.auth.AppUserPrincipal;

/**
 * Ermittelt, wer gerade etwas anlegt: angemeldeter Benutzer (UI)
 * oder API-Key (REST-Zugriff, z.B. durch KI-Agenten).
 */
@Service
public class ErstellerService {

    public String aktuellerErsteller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof AppUserPrincipal p) {
            // Kennung statt Anzeigename: E-Mail, sonst die User-ID des Anbieters
            // (z.B. "bluesky:did:plc:..." oder "github:12345")
            String email = p.getAppUser().getEmail();
            return email != null && !email.isBlank() ? email : p.getAppUser().getUserId().toString();
        }
        // ApiKeyFilter setzt den Namen des API-Keys als Principal
        return "API: " + auth.getName();
    }
}
