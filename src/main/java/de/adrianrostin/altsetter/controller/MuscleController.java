package de.adrianrostin.altsetter.controller;

import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
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

/**
 * Übungen je Muskel. Die Muskeln selbst stehen fest in der App; hier geht es nur um ihre Übungen.
 * Die Sprache kommt aus {@code Accept-Language} (siehe {@code WebConfig#localeResolver}).
 */
@Tag(name = "Muskeln")
@RestController
@RequestMapping(ApiPaths.V1 + "/muscles")
@RequiredArgsConstructor
public class MuscleController {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 100;
    /** Übungen pro Seite, wenn nichts angegeben ist; die App lädt beim Scrollen nach. */
    private static final String PAGE_SIZE = "20";

    private final ExerciseService service;

    @Operation(
            summary = "Übungen für einen Muskel",
            description = "Alle Übungen, die den Muskel als Haupt- oder mitarbeitenden Muskel trainieren, mit allen"
                    + " Angaben wie GET /exercises/{id}, seitenweise. Bei einer Muskelgruppe (z. B. shoulders) zählen"
                    + " alle ihre Anteile. Reihenfolge wie die Karte der App: je Anteil seine Hauptübungen, danach"
                    + " die, bei denen er nur mitarbeitet; jeweils nach Name.")
    @GetMapping("/{id}/exercises")
    public PageResponse<ExerciseDetailDto> exercises(
            @Parameter(description = "Muskel (z. B. side-delts) oder Muskelgruppe (z. B. shoulders)")
                    @PathVariable
                    @Pattern(regexp = ApiPaths.ID_PATTERN) @Size(max = ApiPaths.ID_MAX_LENGTH) String id,
            @Parameter(description = "Ab 0") @RequestParam(defaultValue = "0") @Min(0) @Max(MAX_PAGE) int page,
            @Parameter(description = "Übungen pro Seite")
                    @RequestParam(defaultValue = PAGE_SIZE)
                    @Min(1) @Max(MAX_PAGE_SIZE) int size,
            Locale locale) {
        return PageResponse.of(service.forMuscle(id, Language.of(locale)), page, size);
    }
}
