package de.adrianrostin.altsetter.dto;

import de.adrianrostin.altsetter.entity.Difficulty;
import java.util.List;

/**
 * Eine Übung in Listen: genug, um sie anzuzeigen und am 3D-Modell hervorzuheben.
 *
 * @param id Kennung, in allen Sprachen gleich, z. B. {@code lat-pulldown}
 * @param description kurze Beschreibung in ein bis zwei Sätzen
 * @param primaryMuscles Kennungen der Hauptmuskeln
 * @param secondaryMuscles Kennungen der mitarbeitenden Muskeln
 */
public record ExerciseSummaryDto(
        String id,
        String name,
        String description,
        Difficulty difficulty,
        List<EquipmentDto> equipment,
        List<String> primaryMuscles,
        List<String> secondaryMuscles) {}
