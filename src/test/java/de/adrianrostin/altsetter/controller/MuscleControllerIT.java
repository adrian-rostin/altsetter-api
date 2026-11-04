package de.adrianrostin.altsetter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.IntegrationTest;
import de.adrianrostin.altsetter.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class MuscleControllerIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void listsAllMusclesInBodyOrderWithTheirMeshes() {
        var json = assertThat(mvc.get().uri("/api/v1/muscles")).hasStatusOk().bodyJson();

        json.extractingPath("$.items.length()").isEqualTo(38);
        json.extractingPath("$.items[0].id").isEqualTo("upper-chest");
        json.extractingPath("$.items[0].familyId").isEqualTo("chest");
        json.extractingPath("$.items[0].meshNames").asArray().isNotEmpty();
    }

    @Test
    void namesMusclesInTheRequestedLanguage() {
        assertThat(mvc.get().uri("/api/v1/muscles").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].name")
                .isEqualTo("Upper Chest");
        assertThat(mvc.get().uri("/api/v1/muscles"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].name")
                .isEqualTo("Obere Brust");
    }

    @Test
    void listsFamiliesWithTheirPartsInOrder() {
        assertThat(mvc.get().uri("/api/v1/muscle-families"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[?(@.id == 'shoulders')].muscleIds[*]")
                .asArray()
                .containsExactly("front-delts", "side-delts", "rear-delts");
    }

    @Test
    void answersUnknownPathsWithProblemDetails() {
        assertThat(mvc.get().uri("/api/v1/gibt-es-nicht"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("/api/problems/not-found");
    }

    @Test
    void echoesAValidRequestIdAndReplacesAnInvalidOne() {
        assertThat(mvc.get().uri("/api/v1/muscles").header(RequestIdFilter.HEADER, "abc-12345"))
                .headers()
                .hasValue(RequestIdFilter.HEADER, "abc-12345");

        var response = mvc.get()
                .uri("/api/v1/muscles")
                .header(RequestIdFilter.HEADER, "<script>")
                .exchange()
                .getResponse();
        assertThat(response.getHeader(RequestIdFilter.HEADER))
                .isNotEqualTo("<script>")
                .hasSize(36);
    }

    @Test
    void jsonResponsesAreCacheableAndRevalidatedWithEtag() {
        var first = mvc.get().uri("/api/v1/muscles").exchange();
        assertThat(first).hasStatusOk().headers().hasValue("Cache-Control", "max-age=300, public");
        assertThat(first.getResponse().getHeaders(HttpHeaders.VARY)).contains(HttpHeaders.ACCEPT_LANGUAGE);
        String etag = first.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        assertThat(mvc.get().uri("/api/v1/muscles").header("If-None-Match", etag))
                .hasStatus(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void servesTheOpenApiDescription() {
        assertThat(mvc.get().uri("/api/docs/openapi.json"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.components.schemas.Problem")
                .isNotNull();
    }
}
