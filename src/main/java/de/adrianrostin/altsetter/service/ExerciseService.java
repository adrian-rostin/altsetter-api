package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.exception.NotFoundException;
import de.adrianrostin.altsetter.mapper.ExerciseMapper;
import de.adrianrostin.altsetter.repository.ExerciseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Übungen in einer Sprache. Die Daten ändern sich nur mit einer Migration, deshalb bleibt die
 * sortierte Liste je Sprache im Cache; geblättert wird darin (siehe {@code PageResponse#of}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    public static final String SUMMARY_CACHE = "exercise-summaries";
    public static final String DETAIL_CACHE = "exercise-detail";

    private final ExerciseRepository repository;
    private final ExerciseMapper mapper;
    private final MuscleService muscleService;

    /** Alle Übungen, sortiert nach dem Namen in dieser Sprache, bei Gleichstand nach ID. */
    @Cacheable(SUMMARY_CACHE)
    public List<ExerciseSummaryDto> summaries(Language language) {
        MuscleCatalog catalog = muscleService.catalog(language);
        return repository.findAllOrderedByName(language.getCode()).stream()
                .map(exercise -> mapper.toSummary(exercise, catalog, language))
                .toList();
    }

    @Cacheable(DETAIL_CACHE)
    public ExerciseDetailDto detail(String id, Language language) {
        return repository
                .findById(id)
                .map(exercise -> mapper.toDetail(exercise, muscleService.catalog(language), language))
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.EXERCISE, id));
    }
}
