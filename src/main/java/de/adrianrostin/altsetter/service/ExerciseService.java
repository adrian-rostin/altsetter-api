package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
import de.adrianrostin.altsetter.dto.ExerciseImageDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.entity.Exercise;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.entity.MuscleRole;
import de.adrianrostin.altsetter.exception.NotFoundException;
import de.adrianrostin.altsetter.mapper.ExerciseMapper;
import de.adrianrostin.altsetter.repository.ExerciseRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Übungen in einer Sprache. Die Daten ändern sich selten und nur von Hand, deshalb bleibt die
 * sortierte Liste je Sprache im Cache; geblättert wird darin (siehe {@code PageResponse#of}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    public static final String SUMMARY_CACHE = "exercise-summaries";
    public static final String DETAIL_CACHE = "exercise-detail";
    public static final String MUSCLE_CACHE = "muscle-exercises";

    private final ExerciseRepository repository;
    private final ExerciseMapper mapper;
    private final MuscleService muscleService;
    private final ExerciseImageService imageService;

    /** Alle Übungen, sortiert nach dem Namen in dieser Sprache, bei Gleichstand nach ID. */
    @Cacheable(SUMMARY_CACHE)
    public List<ExerciseSummaryDto> summaries(Language language) {
        MuscleCatalog catalog = muscleService.catalog(language);
        Map<String, List<ExerciseImageDto>> images = imageService.byExercise();
        return repository.findAllOrderedByName(language.getCode()).stream()
                .map(exercise -> mapper.toSummary(exercise, catalog, language, imagesOf(exercise, images)))
                .toList();
    }

    @Cacheable(DETAIL_CACHE)
    public ExerciseDetailDto detail(String id, Language language) {
        return repository
                .findById(id)
                .map(exercise -> mapper.toDetail(
                        exercise,
                        muscleService.catalog(language),
                        language,
                        imagesOf(exercise, imageService.byExercise())))
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.EXERCISE, id));
    }

    /**
     * Alle Übungen, die einen Muskel trainieren, als Haupt- oder mitarbeitenden Muskel, mit allen
     * Angaben wie {@link #detail}. Bei einer Muskelgruppe zählen alle ihre Anteile. Sortiert wie die
     * Karte der App (siehe unten), damit sie die Seiten einfach aneinanderhängen kann; die ganze Liste
     * bleibt im Cache, geblättert wird darin.
     */
    @Cacheable(MUSCLE_CACHE)
    public List<ExerciseDetailDto> forMuscle(String muscleOrFamilyId, Language language) {
        MuscleCatalog catalog = muscleService.catalog(language);
        List<String> parts = catalog.muscleIdsOf(muscleOrFamilyId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.MUSCLE, muscleOrFamilyId));
        List<Exercise> byName = repository.findAllOrderedByName(language.getCode());

        // Reihenfolge wie die Karte der App: je Anteil (von oben nach unten) seine Hauptübungen,
        // danach alle, bei denen ein Anteil nur mitarbeitet; jeweils nach Name, jede Übung einmal.
        Set<Exercise> ordered = new LinkedHashSet<>();
        for (String part : parts) {
            byName.stream()
                    .filter(exercise -> trains(exercise, part, MuscleRole.PRIMARY))
                    .forEach(ordered::add);
        }
        byName.stream()
                .filter(exercise -> parts.stream().anyMatch(part -> trains(exercise, part, null)))
                .forEach(ordered::add);

        Map<String, List<ExerciseImageDto>> images = imageService.byExercise();
        return ordered.stream()
                .map(exercise -> mapper.toDetail(exercise, catalog, language, imagesOf(exercise, images)))
                .toList();
    }

    /** Trainiert die Übung diesen Muskel (in dieser Rolle; {@code null}: in irgendeiner)? */
    private static boolean trains(Exercise exercise, String muscleId, MuscleRole role) {
        return exercise.getMuscles().stream()
                .anyMatch(muscle -> muscle.muscleId().equals(muscleId) && (role == null || muscle.role() == role));
    }

    private static List<ExerciseImageDto> imagesOf(Exercise exercise, Map<String, List<ExerciseImageDto>> images) {
        return images.getOrDefault(exercise.getId(), List.of());
    }
}
