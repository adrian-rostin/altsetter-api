package de.adrianrostin.altsetter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gibt jeder Anfrage eine ID: aus dem Header {@code X-Request-Id}, falls ein Proxy schon eine
 * vergeben hat, sonst eine neue. Sie steht in jeder Logzeile, in jeder Antwort und in jeder
 * Fehlermeldung, damit sich ein Fehler im Log wiederfinden lässt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /** Nur harmlose Zeichen übernehmen, damit niemand Text ins Log schmuggelt. */
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = requestId(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static String requestId(String fromHeader) {
        if (fromHeader != null && VALID.matcher(fromHeader).matches()) {
            return fromHeader;
        }
        return UUID.randomUUID().toString();
    }
}
