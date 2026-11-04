package de.adrianrostin.altsetter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Security-Header für alle Antworten. Die API liefert nur JSON: nichts darf darin Skripte laden,
 * eingebettet werden oder von fremden Seiten gelesen werden. Ausnahme ist die Swagger-UI unter
 * {@code /api/docs} (ausgeliefert unter {@code /api/swagger-ui}, nur lokal eingeschaltet), die als
 * Webseite eigene Skripte und Styles braucht.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final List<String> DOCS_PATHS = List.of("/api/docs", "/api/swagger-ui");

    private static final String API_CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    private static final String DOCS_CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", isDocs(request.getRequestURI()) ? DOCS_CSP : API_CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        chain.doFilter(request, response);
    }

    private static boolean isDocs(String path) {
        return DOCS_PATHS.stream().anyMatch(path::startsWith);
    }
}
