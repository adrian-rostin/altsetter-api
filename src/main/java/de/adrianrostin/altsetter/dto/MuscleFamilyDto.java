package de.adrianrostin.altsetter.dto;

import java.util.List;

/**
 * Eine Muskelgruppe, z. B. die Schulter.
 *
 * @param id Kennung, in allen Sprachen gleich, z. B. {@code shoulders}
 * @param muscleIds Kennungen ihrer Anteile in fester Reihenfolge
 */
public record MuscleFamilyDto(
        String id, String name, String latinName, String region, String summary, List<String> muscleIds) {}
