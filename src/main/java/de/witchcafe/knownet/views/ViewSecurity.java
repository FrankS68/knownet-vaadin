package de.witchcafe.knownet.views;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.knownet.KnownetRoles;

/**
 * Hilfsmethoden fuer rollenbasierte UI-Entscheidungen.
 */
public final class ViewSecurity {

    private ViewSecurity() {}

    public static boolean kannBearbeiten(CurrentUser currentUser) {
        return hatRolle(currentUser, KnownetRoles.AUTOR) || hatRolle(currentUser, "ADMIN");
    }

    public static boolean kannKommentieren(CurrentUser currentUser) {
        return hatRolle(currentUser, KnownetRoles.KOMMENTATOR)
            || hatRolle(currentUser, KnownetRoles.AUTOR)
            || hatRolle(currentUser, "ADMIN");
    }

    public static boolean istAdmin(CurrentUser currentUser) {
        return hatRolle(currentUser, "ADMIN");
    }

    public static boolean istAngemeldet(CurrentUser currentUser) {
        return currentUser.get().isPresent();
    }

    private static boolean hatRolle(CurrentUser currentUser, String rolle) {
        return currentUser.getPrincipal()
                .map(p -> p.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + rolle)))
                .orElse(false);
    }
}
