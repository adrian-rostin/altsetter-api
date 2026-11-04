package de.adrianrostin.altsetter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class StatusControllerIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void showsTheApiVersionAndWhereToGoNext() {
        var result = assertThat(mvc.get().uri("/api"));

        result.hasStatusOk().headers().hasValue("Cache-Control", "no-store");
        var json = result.bodyJson();
        json.extractingPath("$.version").isEqualTo("v1");
        json.extractingPath("$.endpoints").isEqualTo("/api/v1");
        json.extractingPath("$.health").isEqualTo("/api/health");
    }

    @Test
    void reportsUpWhenTheDatabaseAnswers() {
        assertThat(mvc.get().uri("/api/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }
}
