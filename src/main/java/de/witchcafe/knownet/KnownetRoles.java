package de.witchcafe.knownet;

/**
 * Knownet-spezifische Rollen, ergaenzend zu de.witchcafe.auth.AppRoles (ADMIN, USER).
 *
 * ADMIN    (aus witch-auth) - voller Zugriff inkl. Benutzerverwaltung
 * AUTOR    - darf Quellen und Aussagen anlegen und bearbeiten
 * KOMMENTATOR - darf Kommentare zu Quellen und Aussagen verfassen
 * USER     (aus witch-auth) - nur lesend (ohne Kommentare)
 * anonym   - Quellen und Aussagen lesen (kein Login noetig)
 */
public final class KnownetRoles {

    private KnownetRoles() {}

    /** Darf Quellen, Aussagen und Verknuepfungen anlegen und bearbeiten. */
    public static final String AUTOR = "AUTOR";

    /** Darf Kommentare zu Quellen und Aussagen verfassen. */
    public static final String KOMMENTATOR = "KOMMENTATOR";
}
