package de.adrianrostin.altsetter.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.adrianrostin.altsetter.config.RateLimitProperties;
import de.adrianrostin.altsetter.exception.ProblemType;
import de.adrianrostin.altsetter.exception.Problems;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Begrenzt die Anfragen pro Besucher (Token Bucket je IP). Normale Nutzung merkt davon nichts: Ein
 * Seitenaufruf braucht drei Anfragen, der Puffer reicht für hunderte. Jede Antwort sagt per
 * {@code RateLimit-*}-Header, wie viel noch übrig ist.
 *
 * <p>Die IP liegt nur im Arbeitsspeicher und fällt heraus, sobald der Eimer wieder voll wäre
 * (höchstens zehn Minuten).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBooleanProperty(name = "altsetter.rate-limit.enabled", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String LIMIT_HEADER = "RateLimit-Limit";
    public static final String REMAINING_HEADER = "RateLimit-Remaining";
    public static final String RESET_HEADER = "RateLimit-Reset";
    public static final String CLOUDFLARE_CLIENT_IP = "CF-Connecting-IP";

    private static final Duration MAX_RETENTION = Duration.ofMinutes(10);
    private static final long MAX_VISITORS = 100_000;

    private final RateLimitProperties properties;
    private final JsonMapper jsonMapper;
    private final Problems problems;
    private final LocaleResolver localeResolver;
    private final Bandwidth bandwidth;
    private final List<IpRange> cloudflareRanges;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(
            RateLimitProperties properties,
            JsonMapper jsonMapper,
            Problems problems,
            LocaleResolver localeResolver) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.problems = problems;
        this.localeResolver = localeResolver;
        this.bandwidth = Bandwidth.builder()
                .capacity(properties.capacity())
                .refillGreedy(properties.refillTokens(), properties.refillPeriod())
                .build();
        this.cloudflareRanges =
                properties.cloudflareRanges().stream().map(IpRange::parse).toList();
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(retention(properties))
                .maximumSize(MAX_VISITORS)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = buckets.get(
                clientIp(request), ip -> Bucket.builder().addLimit(bandwidth).build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, String.valueOf(properties.capacity()));
        response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
        response.setHeader(RESET_HEADER, String.valueOf(ceilSeconds(probe.getNanosToWaitForReset())));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            reject(request, response, Math.max(1, ceilSeconds(probe.getNanosToWaitForRefill())));
        }
    }

    /**
     * Die IP des Besuchers. Proxys im Cluster löst Tomcat schon auf ({@code server.forward-headers-strategy}).
     * Steht davor Cloudflare, kommt die Anfrage von einer Cloudflare-IP; dann zählt deren Angabe, aber
     * nur dann, sonst könnte jeder den Header selbst setzen.
     */
    String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader(CLOUDFLARE_CLIENT_IP);
        if (properties.trustCloudflareHeader()
                && forwarded != null
                && IpRange.isAddress(forwarded.strip())
                && cloudflareRanges.stream().anyMatch(range -> range.contains(remote))) {
            return forwarded.strip();
        }
        return remote;
    }

    /**
     * Antwortet wie alle anderen Fehler der API: Problem Details mit Typ, Pfad und Request-ID, in der
     * Sprache des Requests (der Filter läuft vor Spring MVC und löst sie deshalb selbst auf).
     */
    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        ProblemDetail problem = problems.create(
                ProblemType.RATE_LIMITED,
                localeResolver.resolveLocale(request),
                "detail.rate-limited",
                retryAfterSeconds);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", request.getRequestURI());
        if (problem.getProperties() != null) {
            body.putAll(problem.getProperties());
        }

        response.setStatus(problem.getStatus());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body);
    }

    /** Solange braucht ein leerer Eimer, bis er wieder voll ist; danach ist ein neuer gleichwertig. */
    private static Duration retention(RateLimitProperties properties) {
        Duration untilFull = properties
                .refillPeriod()
                .multipliedBy(properties.capacity())
                .dividedBy(Math.max(1, properties.refillTokens()));
        return untilFull.compareTo(MAX_RETENTION) > 0 ? MAX_RETENTION : untilFull.plusSeconds(1);
    }

    private static long ceilSeconds(long nanos) {
        return Math.max(0, (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
    }
}
