package de.adrianrostin.altsetter.controller;

import de.adrianrostin.altsetter.dto.ExerciseDetailDto;
import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.dto.PageResponse;
import de.adrianrostin.altsetter.entity.CodedEnum;
import de.adrianrostin.altsetter.entity.ImagePosition;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.service.ExerciseImageService;
import de.adrianrostin.altsetter.service.ExerciseImageService.ImageFile;
import de.adrianrostin.altsetter.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final String POSITION = ImagePosition.PATTERN;

    /** Bilder mit Version in der URL ändern sich nie: Ein neues Bild bekommt eine neue URL. */
    private static final CacheControl IMMUTABLE =
            CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private static final CacheControl UNVERSIONED =
            CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    private final ExerciseService service;
    private final ExerciseImageService images;

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

    @Operation(
            summary = "Foto einer Übung",
            description = "Start- oder Endstellung als WebP. Die URL samt Version steht in `images` der Übung;"
                    + " mit passender Version darf der Browser das Bild unbegrenzt behalten.")
    @GetMapping("/{id}/images/{position}")
    public ResponseEntity<byte[]> image(
            @PathVariable @Pattern(regexp = ApiPaths.ID_PATTERN) @Size(max = ApiPaths.ID_MAX_LENGTH) String id,
            @Parameter(description = "`start` oder `end`") @PathVariable @Pattern(regexp = POSITION) String position,
            @Parameter(description = "Version aus der URL in `images`") @RequestParam(required = false) String v) {
        ImageFile file = images.file(id, CodedEnum.fromCode(ImagePosition.class, position));
        // Ohne (passende) Version könnte sich das Bild unter derselben URL noch ändern.
        CacheControl cacheControl = file.version().equals(v) ? IMMUTABLE : UNVERSIONED;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .cacheControl(cacheControl)
                .body(file.data());
    }
}
