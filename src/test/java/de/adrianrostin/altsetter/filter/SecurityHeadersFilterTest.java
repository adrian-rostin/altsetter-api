package de.adrianrostin.altsetter.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void locksDownJsonResponses() throws Exception {
        MockHttpServletResponse response = call("/api/v1/muscles");

        assertThat(response.getHeader("Content-Security-Policy")).startsWith("default-src 'none'");
        assertThat(response.getHeader("Strict-Transport-Security")).contains("max-age=31536000");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Cross-Origin-Resource-Policy")).isEqualTo("same-origin");
    }

    @Test
    void letsTheSwaggerUiLoadItsOwnScriptsAndStyles() throws Exception {
        for (String path : new String[] {"/api/docs", "/api/swagger-ui/index.html"}) {
            assertThat(call(path).getHeader("Content-Security-Policy"))
                    .contains("script-src 'self'")
                    .contains("frame-ancestors 'none'");
        }
    }

    private MockHttpServletResponse call(String uri) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", uri), response, new MockFilterChain());
        return response;
    }
}
