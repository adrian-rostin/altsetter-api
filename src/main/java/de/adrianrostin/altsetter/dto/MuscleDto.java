package de.adrianrostin.altsetter.dto;

import java.util.List;

/**
 * Ein Muskel mit allem, was die App für das 3D-Modell braucht.
 *
 * @param id Kennung, in allen Sprachen gleich, z. B. {@code upper-chest}
 * @param familyId Muskelgruppe, z. B. {@code chest}; {@code null} bei eigenständigen Muskeln
 * @param meshNames Mesh-Namen im 3D-Modell ohne Seitenangabe
 */
public record MuscleDto(
        String id,
        String familyId,
        String name,
        String latinName,
        String region,
        String summary,
        List<String> meshNames) {

    public MuscleRef toRef() {
        return new MuscleRef(id, name);
    }
}
