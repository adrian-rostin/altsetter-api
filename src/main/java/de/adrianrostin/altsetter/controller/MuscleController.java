package de.adrianrostin.altsetter.controller;

import de.adrianrostin.altsetter.dto.ListResponse;
import de.adrianrostin.altsetter.dto.MuscleDto;
import de.adrianrostin.altsetter.dto.MuscleFamilyDto;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.service.MuscleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Die Sprache kommt aus {@code Accept-Language} (siehe {@code WebConfig#localeResolver}). */
@Tag(name = "Muskeln")
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class MuscleController {

    private final MuscleService service;

    @Operation(summary = "Alle Muskelgruppen", description = "Von oben nach unten am Körper.")
    @GetMapping("/muscle-families")
    public ListResponse<MuscleFamilyDto> families(Locale locale) {
        return ListResponse.of(service.catalog(Language.of(locale)).families());
    }

    @Operation(summary = "Alle Muskeln", description = "Von oben nach unten am Körper, mit den Namen im 3D-Modell.")
    @GetMapping("/muscles")
    public ListResponse<MuscleDto> muscles(Locale locale) {
        return ListResponse.of(service.catalog(Language.of(locale)).muscles());
    }
}
