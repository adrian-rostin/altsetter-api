package de.adrianrostin.altsetter.controller;

/** Gemeinsame Pfade und Formate der API. */
public final class ApiPaths {

    /** Alle Endpunkte liegen unter dieser Version. Brechende Änderungen bekommen eine neue. */
    public static final String V1 = "/api/v1";

    /** Kennungen sind kleingeschrieben, mit Bindestrichen, z. B. {@code upper-chest}. */
    public static final String ID_PATTERN = "[a-z0-9]+(-[a-z0-9]+)*";

    public static final int ID_MAX_LENGTH = 64;

    private ApiPaths() {}
}
