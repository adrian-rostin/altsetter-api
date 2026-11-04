package de.adrianrostin.altsetter.controller;

import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.dto.PageResponse;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Die Sprache kommt aus {@code Accept-Language} (siehe {@code WebConfig#localeResolver}). */
@Tag(name = "Übungen")
@RestController
@RequestMapping(ApiPaths.V1 + "/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    /** Größte Seite: groß genug, dass die App den ganzen Katalog in zwei Abrufen hat. */
    static final int MAX_PAGE_SIZE = 100;

    private static final int MAX_PAGE = 10_000;

    private final ExerciseService service;

    @Operation(summary = "Alle Übungen", description = "Seitenweise, sortiert nach dem Namen in der gewählten Sprache.")
    @GetMapping
    public PageResponse<ExerciseSummaryDto> list(
            @Parameter(description = "Ab 0") @RequestParam(defaultValue = "0") @Min(0) @Max(MAX_PAGE) int page,
            @Parameter(description = "Einträge pro Seite")
                    @RequestParam(defaultValue = "20")
                    @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Locale locale) {
        return PageResponse.of(service.summaries(Language.of(locale)), page, size);
    }

    @Operation(summary = "Eine Übung", description = "Mit Muskelnamen und Hinweisen zur Ausführung.")
    @GetMapping("/{id}")
    public ExerciseDetailDto detail(
            @PathVariable @Pattern(regexp = ApiPaths.ID_PATTERN) @Size(max = ApiPaths.ID_MAX_LENGTH) String id,
            Locale locale) {
        return service.detail(id, Language.of(locale));
    }
}
