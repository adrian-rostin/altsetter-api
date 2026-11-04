package de.adrianrostin.altsetter.filter;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.config.RateLimitProperties;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.exception.Problems;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private static final int CAPACITY = 3;
    private static final String CLOUDFLARE_NODE = "172.70.0.1";

    private final RateLimitFilter filter = new RateLimitFilter(
            new RateLimitProperties(true, CAPACITY, 1, Duration.ofMinutes(1), true, List.of("172.64.0.0/13")),
            JsonMapper.builder().build(),
            new Problems(messages()),
            localeResolver());

    @Test
    void letsABurstUpToTheCapacityThroughAndReportsWhatIsLeft() throws Exception {
        for (int i = 0; i < CAPACITY; i++) {
            MockHttpServletResponse response = call("/api/v1/muscles", "203.0.113.1", null);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader(RateLimitFilter.LIMIT_HEADER)).isEqualTo(String.valueOf(CAPACITY));
            assertThat(response.getHeader(RateLimitFilter.REMAINING_HEADER))
                    .isEqualTo(String.valueOf(CAPACITY - 1 - i));
        }
    }

    @Test
    void answersFurtherRequestsWith429ProblemDetailsAndRetryAfter() throws Exception {
        exhaust("203.0.113.2", null);

        MockHttpServletResponse response = call("/api/v1/muscles", "203.0.113.2", null);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isBetween(1, 60);
        assertThat(Integer.parseInt(response.getHeader(RateLimitFilter.RESET_HEADER)))
                .isBetween(1, 180);
        assertThat(response.getContentAsString())
                .contains("\"type\":\"/api/problems/rate-limited\"")
                .contains("Zu viele Anfragen");
    }

    @Test
    void answersInTheRequestedLanguage() throws Exception {
        exhaust("203.0.113.8", null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/muscles");
        request.setRemoteAddr("203.0.113.8");
        request.addHeader("Accept-Language", "en-US,en;q=0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getContentAsString())
                .contains("\"title\":\"Too many requests\"")
                .contains("Please try again in");
    }

    @Test
    void countsEveryVisitorSeparately() throws Exception {
        exhaust("203.0.113.3", null);

        assertThat(call("/api/v1/muscles", "203.0.113.4", null).getStatus()).isEqualTo(200);
    }

    @Test
    void identifiesVisitorsBehindCloudflareByTheirOwnIp() throws Exception {
        exhaust(CLOUDFLARE_NODE, "198.51.100.1");

        assertThat(call("/api/v1/muscles", CLOUDFLARE_NODE, "198.51.100.1").getStatus())
                .isEqualTo(429);
        assertThat(call("/api/v1/muscles", CLOUDFLARE_NODE, "198.51.100.2").getStatus())
                .isEqualTo(200);
    }

    @Test
    void ignoresTheCloudflareHeaderFromAnyoneElse() throws Exception {
        exhaust("203.0.113.6", "198.51.100.3");

        assertThat(call("/api/v1/muscles", "203.0.113.6", "198.51.100.4").getStatus())
                .isEqualTo(429);
    }

    @Test
    void ignoresACloudflareHeaderThatIsNoIpAddress() throws Exception {
        exhaust(CLOUDFLARE_NODE, "not-an-ip");

        assertThat(call("/api/v1/muscles", CLOUDFLARE_NODE, "something-else").getStatus())
                .as("beide zählen als die Cloudflare-IP selbst")
                .isEqualTo(429);
    }

    @Test
    void rejectedResponsesAreNeverCached() throws Exception {
        exhaust("203.0.113.7", null);

        MockHttpServletResponse response = call("/api/v1/muscles", "203.0.113.7", null);

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).contains("\"instance\":\"/api/v1/muscles\"");
    }

    @Test
    void ignoresRequestsOutsideTheApi() throws Exception {
        exhaust("203.0.113.5", null);

        assertThat(call("/actuator/health", "203.0.113.5", null).getStatus()).isEqualTo(200);
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        return messages;
    }

    private static AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(Language.supportedLocales());
        resolver.setDefaultLocale(Language.DEFAULT.getLocale());
        return resolver;
    }

    private void exhaust(String remoteAddress, String cloudflareIp) throws ServletException, IOException {
        for (int i = 0; i < CAPACITY; i++) {
            call("/api/v1/muscles", remoteAddress, cloudflareIp);
        }
    }

    private MockHttpServletResponse call(String uri, String remoteAddress, String cloudflareIp)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddress);
        if (cloudflareIp != null) {
            request.addHeader(RateLimitFilter.CLOUDFLARE_CLIENT_IP, cloudflareIp);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
