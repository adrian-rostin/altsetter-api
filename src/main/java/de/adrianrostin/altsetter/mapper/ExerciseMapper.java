package de.adrianrostin.altsetter.mapper;

import de.adrianrostin.altsetter.dto.EquipmentDto;
import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.dto.MuscleDto;
import de.adrianrostin.altsetter.dto.MuscleRef;
import de.adrianrostin.altsetter.entity.Exercise;
import de.adrianrostin.altsetter.entity.ExerciseText;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.entity.MuscleRole;
import de.adrianrostin.altsetter.entity.TipKind;
import de.adrianrostin.altsetter.service.MuscleCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Wandelt Übungen in die DTOs der API. Muss innerhalb einer Transaktion laufen (Lazy Loading).
 * {@code catalog} ist der Muskelkatalog in derselben Sprache: Er sortiert Muskeln in
 * Körperreihenfolge und liefert ihre Namen.
 */
@Component
@RequiredArgsConstructor
public class ExerciseMapper {

    private final EquipmentMapper equipmentMapper;

    public ExerciseSummaryDto toSummary(Exercise exercise, MuscleCatalog catalog, Language language) {
        ExerciseText text = exercise.text(language);
        return new ExerciseSummaryDto(
                exercise.getId(),
                text.name(),
                text.description(),
                exercise.getDifficulty(),
                equipment(exercise, language),
                exercise.muscleIds(MuscleRole.PRIMARY, catalog.muscleOrder()),
                exercise.muscleIds(MuscleRole.SECONDARY, catalog.muscleOrder()));
    }

    public ExerciseDetailDto toDetail(Exercise exercise, MuscleCatalog catalog, Language language) {
        ExerciseText text = exercise.text(language);
        return new ExerciseDetailDto(
                exercise.getId(),
                text.name(),
                text.description(),
                exercise.getDifficulty(),
                equipment(exercise, language),
                new ExerciseDetailDto.Muscles(
                        muscleRefs(exercise, MuscleRole.PRIMARY, catalog),
                        muscleRefs(exercise, MuscleRole.SECONDARY, catalog)),
                exercise.tips(TipKind.CUE, language),
                exercise.tips(TipKind.MISTAKE, language));
    }

    private List<EquipmentDto> equipment(Exercise exercise, Language language) {
        return equipmentMapper.toDtos(exercise.getEquipment(), language);
    }

    private static List<MuscleRef> muscleRefs(Exercise exercise, MuscleRole role, MuscleCatalog catalog) {
        return exercise.muscleIds(role, catalog.muscleOrder()).stream()
                .map(catalog::muscle)
                .map(MuscleDto::toRef)
                .toList();
    }
}
