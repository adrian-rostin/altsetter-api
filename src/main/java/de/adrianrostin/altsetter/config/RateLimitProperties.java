package de.adrianrostin.altsetter.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anfragen pro Besucher (Token Bucket).
 *
 * @param capacity so viele Anfragen gehen auf einmal, z. B. beim ersten Seitenaufruf
 * @param refillTokens so viele kommen je {@code refillPeriod} wieder dazu
 * @param trustCloudflareHeader Besucher hinter Cloudflare an {@code CF-Connecting-IP} erkennen, aber
 *     nur, wenn die Anfrage wirklich aus {@code cloudflareRanges} kommt
 * @param cloudflareRanges IP-Bereiche von Cloudflare (https://www.cloudflare.com/ips/)
 */
@ConfigurationProperties("altsetter.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int capacity,
        int refillTokens,
        Duration refillPeriod,
        boolean trustCloudflareHeader,
        List<String> cloudflareRanges) {

    public RateLimitProperties {
        cloudflareRanges = cloudflareRanges == null ? List.of() : List.copyOf(cloudflareRanges);
    }
}
