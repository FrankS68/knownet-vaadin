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
            String name = p.getAppUser().getFullName();
            return name != null && !name.isBlank() ? name : p.getAppUser().getPreferredUsername();
        }
        // ApiKeyFilter setzt den Namen des API-Keys als Principal
        return "API: " + auth.getName();
    }
}
