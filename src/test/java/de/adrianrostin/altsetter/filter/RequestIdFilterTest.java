package de.adrianrostin.altsetter.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RequestIdFilterTest {

    @Test
    void keepsAnIdFromTheProxy() {
        assertThat(RequestIdFilter.requestId("3f2a9c1e-traefik")).isEqualTo("3f2a9c1e-traefik");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "kurz", "mit leerzeichen", "zeile\numbruch", "<script>alert(1)</script>"})
    void generatesAFreshIdOtherwise(String fromHeader) {
        String id = RequestIdFilter.requestId(fromHeader);

        assertThat(UUID.fromString(id)).isNotNull();
    }
}
