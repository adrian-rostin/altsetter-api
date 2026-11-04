package de.adrianrostin.altsetter.controller;

import de.adrianrostin.altsetter.service.HealthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Einstieg der API: welche Version läuft und ob sie gerade funktioniert. */
@Tag(name = "Status")
@RestController
@RequestMapping("/api")
public class StatusController {

    private final HealthService healthService;
    private final String build;

    public StatusController(HealthService healthService, ObjectProvider<BuildProperties> buildProperties) {
        this.healthService = healthService;
        BuildProperties properties = buildProperties.getIfAvailable();
        this.build = properties == null ? "unbekannt" : properties.getVersion();
    }

    /**
     * @param version Version der Endpunkte, wie im Pfad ({@code /api/v1})
     * @param build Version der laufenden Anwendung
     */
    public record ApiInfo(String name, String version, String build, String endpoints, String health) {}

    /** @param status {@code UP} oder {@code DOWN} */
    public record Health(String status) {}

    @Operation(summary = "Version und Einstiegspunkte der API")
    @GetMapping
    public ResponseEntity<ApiInfo> info() {
        ApiInfo info = new ApiInfo("Altsetter API", "v1", build, ApiPaths.V1, "/api/health");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(info);
    }

    /** Dasselbe mit Schrägstrich am Ende; nicht doppelt in der Doku. */
    @Hidden
    @GetMapping("/")
    public ResponseEntity<ApiInfo> infoWithSlash() {
        return info();
    }

    @Operation(
            summary = "Funktioniert die API?",
            description = "UP, wenn die API läuft und die Datenbank mit Daten antwortet; sonst DOWN mit 503.")
    @GetMapping("/health")
    public ResponseEntity<Health> health() {
        boolean healthy = healthService.isHealthy();
        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(new Health(healthy ? "UP" : "DOWN"));
    }
}
