package de.adrianrostin.altsetter.exception;

import java.net.URI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Alle Fehlerarten der API. Der {@code type} jeder Fehlerantwort (RFC 9457) ist eine feste URI wie
 * {@code /api/problems/not-found}; Clients unterscheiden Fehler daran, nicht am Text. Titel und
 * Meldungen stehen je Sprache in {@code messages*.properties} (siehe {@link Problems}).
 */
@Getter
@RequiredArgsConstructor
public enum ProblemType {
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND),
    INVALID_PARAMETER("invalid-parameter", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("method-not-allowed", HttpStatus.METHOD_NOT_ALLOWED),
    RATE_LIMITED("rate-limited", HttpStatus.TOO_MANY_REQUESTS),
    UNAVAILABLE("unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("internal-error", HttpStatus.INTERNAL_SERVER_ERROR);

    private static final String BASE = "/api/problems/";

    private final String code;
    private final HttpStatus status;

    public URI uri() {
        return URI.create(BASE + code);
    }
}
