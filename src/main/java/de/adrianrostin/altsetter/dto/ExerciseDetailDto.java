package de.adrianrostin.altsetter.dto;

import de.adrianrostin.altsetter.entity.Difficulty;
import java.util.List;

/**
 * Alle Angaben zu einer Übung für die Detailseite.
 *
 * @param cues „Darauf achten“
 * @param mistakes „Häufige Fehler“
 * @param images wie in {@link ExerciseSummaryDto#images()}
 */
public record ExerciseDetailDto(
        String id,
        String name,
        String description,
        Difficulty difficulty,
        List<EquipmentDto> equipment,
        Muscles muscles,
        List<String> cues,
        List<String> mistakes,
        List<ExerciseImageDto> images) {

    public record Muscles(List<MuscleRef> primary, List<MuscleRef> secondary) {}
}
