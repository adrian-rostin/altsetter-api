package de.adrianrostin.altsetter.exception;

import de.adrianrostin.altsetter.filter.RequestIdFilter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Baut Problem Details (RFC 9457) in der Sprache des Requests. Titel kommen aus
 * {@code problem.<type>}, Meldungen aus {@code detail.*} in {@code messages*.properties}.
 */
@Component
@RequiredArgsConstructor
public class Problems {

    private final MessageSource messages;

    /** Problem Details dieser Art mit Meldung {@code detailKey}, dazu Typ, Titel und Request-ID. */
    public ProblemDetail create(ProblemType type, Locale locale, String detailKey, Object... args) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.getStatus(), message(detailKey, locale, args));
        decorate(problem, type, locale);
        return problem;
    }

    /** Setzt Typ, Titel und Request-ID an vorhandenen Problem Details. */
    public void decorate(ProblemDetail problem, ProblemType type, Locale locale) {
        problem.setType(type.uri());
        problem.setTitle(message("problem." + type.getCode(), locale));
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
    }

    public String message(String key, Locale locale, Object... args) {
        return messages.getMessage(key, args, locale);
    }
}
