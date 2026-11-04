package de.adrianrostin.altsetter.exception;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Übersetzt Exceptions in Problem Details (RFC 9457). Jede Antwort hat einen festen {@code type}
 * (siehe {@link ProblemType}), Titel und Meldung in der Sprache des Requests ({@code Accept-Language}) und
 * die Request-ID. Ungültige Parameter werden
 * zusätzlich unter {@code errors} einzeln aufgeführt.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Wie lange ein Client bei Überlast warten soll, in Sekunden. */
    private static final String RETRY_AFTER_SECONDS = "2";

    private final Problems problems;

    @ExceptionHandler
    ProblemDetail handleNotFound(NotFoundException exception) {
        return problems.create(
                ProblemType.NOT_FOUND,
                locale(),
                "detail.not-found." + exception.getResource().getKey(),
                exception.getId());
    }

    /**
     * Die Datenbank ist gerade ausgelastet: keine freie Verbindung innerhalb des Timeouts, oder eine
     * Abfrage hat ihr Zeitlimit gerissen.
     */
    @ExceptionHandler({TransientDataAccessException.class, CannotCreateTransactionException.class})
    ResponseEntity<ProblemDetail> handleUnavailable(Exception exception) {
        log.warn("Datenbank nicht rechtzeitig verfügbar: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problems.create(ProblemType.UNAVAILABLE, locale(), "detail.unavailable"));
    }

    /** Alles Unerwartete: ins Log, nach außen nur eine neutrale Meldung. */
    @ExceptionHandler
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unerwarteter Fehler", exception);
        return problems.create(ProblemType.INTERNAL_ERROR, locale(), "detail.internal-error");
    }

    /** Ungültige Felder eines gebundenen Objekts, z. B. {@code ?difficulty=extrem} im Übungsfilter. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ParameterError> errors = exception.getFieldErrors().stream()
                .map(error -> new ParameterError(error.getField(), error.getRejectedValue()))
                .toList();
        return invalidParameters(exception, errors, headers, status, request);
    }

    /** Verletzte Constraints an einzelnen Parametern, z. B. {@code ?size=500}. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ParameterError> errors = exception.getParameterValidationResults().stream()
                .flatMap(ApiExceptionHandler::parameterErrors)
                .toList();
        return invalidParameters(exception, errors, headers, status, request);
    }

    /** Beim Filterobjekt zählen dessen einzelne Felder, sonst der Parameter selbst. */
    private static Stream<ParameterError> parameterErrors(ParameterValidationResult result) {
        if (result instanceof ParameterErrors bound) {
            return bound.getFieldErrors().stream()
                    .map(error -> new ParameterError(error.getField(), error.getRejectedValue()));
        }
        return Stream.of(new ParameterError(result.getMethodParameter().getParameterName(), result.getArgument()));
    }

    /** Nicht umwandelbare Werte, z. B. Text statt Zahl bei {@code ?page=}. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ParameterError> errors = List.of(new ParameterError(exception.getPropertyName(), exception.getValue()));
        return invalidParameters(exception, errors, headers, status, request);
    }

    /** Alle übrigen Standardfälle (unbekannter Pfad, falsche Methode …) bekommen ebenfalls Typ und Request-ID. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Spring reicht bei seinen Standardfällen keinen Body herein, sondern holt ihn erst später aus der Exception.
        if (body == null && exception instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problem
                && (problem.getType() == null
                        || "about:blank".equals(problem.getType().toString()))) {
            problems.decorate(problem, typeFor(status), locale());
            if (exception instanceof NoResourceFoundException) {
                problem.setDetail(problems.message("detail.not-found.path", locale()));
            } else if (exception instanceof HttpRequestMethodNotSupportedException) {
                problem.setDetail(problems.message("detail.method-not-allowed", locale()));
            }
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    private ResponseEntity<Object> invalidParameters(
            Exception exception,
            List<ParameterError> errors,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String names = String.join(
                "', '",
                errors.stream().map(ParameterError::parameter).distinct().toList());
        ProblemDetail problem = names.isEmpty()
                ? problems.create(ProblemType.INVALID_PARAMETER, locale(), "detail.invalid-parameter")
                : problems.create(ProblemType.INVALID_PARAMETER, locale(), "detail.invalid-parameter.named", names);
        problem.setProperty("errors", errors.stream().map(ParameterError::toMap).toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    /** Die Sprache des Requests, aufgelöst von {@code WebConfig#localeResolver}. */
    private static Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    private static ProblemType typeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ProblemType.INVALID_PARAMETER;
            case 404 -> ProblemType.NOT_FOUND;
            case 405 -> ProblemType.METHOD_NOT_ALLOWED;
            case 503 -> ProblemType.UNAVAILABLE;
            default -> ProblemType.INTERNAL_ERROR;
        };
    }

    /** Ein abgelehnter Parameter mit seinem Wert, so wie er ankam. */
    private record ParameterError(String parameter, Object value) {

        Map<String, Object> toMap() {
            return value == null ? Map.of("parameter", parameter) : Map.of("parameter", parameter, "value", value);
        }
    }
}
