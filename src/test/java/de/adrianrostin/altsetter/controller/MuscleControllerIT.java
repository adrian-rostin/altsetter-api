package de.adrianrostin.altsetter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.IntegrationTest;
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
    void listsAFamilyLikeTheAppsCardWithDetails() {
        var json = assertThat(mvc.get().uri("/api/v1/muscles/shoulders/exercises"))
                .hasStatusOk()
                .bodyJson();

        // Per part from top to bottom its main exercises (front delts: none, side delts, rear delts),
        // then the ones a part only works along in: bench press trains the front delts along.
        json.extractingPath("$.items[*].id")
                .asArray()
                .containsExactly("dumbbell-lateral-raise", "reverse-dumbbell-fly", "barbell-bench-press");
        json.extractingPath("$.items[2].cues.length()").isEqualTo(2);
        json.extractingPath("$.items[2].muscles.primary[0].name").isEqualTo("Mittlere Brust");
        json.extractingPath("$.items[2].images[*].position").asArray().containsExactly("start", "end");
        json.extractingPath("$.totalItems").isEqualTo(3);
    }

    @Test
    void pagesThroughTheList() {
        var json = assertThat(mvc.get().uri("/api/v1/muscles/shoulders/exercises?page=2&size=1"))
                .hasStatusOk()
                .bodyJson();

        json.extractingPath("$.items[*].id").asArray().containsExactly("barbell-bench-press");
        json.extractingPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void listsTheExercisesOfASingleMuscle() {
        assertThat(mvc.get().uri("/api/v1/muscles/side-delts/exercises"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[*].id")
                .asArray()
                .containsExactly("dumbbell-lateral-raise");
    }

    @Test
    void keepsTheOrderByPartInEnglish() {
        var result = assertThat(
                mvc.get().uri("/api/v1/muscles/shoulders/exercises").header(HttpHeaders.ACCEPT_LANGUAGE, "en"));

        result.hasStatusOk().headers().hasValue(HttpHeaders.CONTENT_LANGUAGE, "en");
        result.bodyJson()
                .extractingPath("$.items[*].id")
                .asArray()
                .containsExactly("dumbbell-lateral-raise", "reverse-dumbbell-fly", "barbell-bench-press");
    }

    @Test
    void answersUnknownMusclesWith404() {
        var result = assertThat(mvc.get().uri("/api/v1/muscles/calves/exercises"));

        result.hasStatus(HttpStatus.NOT_FOUND);
        var json = result.bodyJson();
        json.extractingPath("$.type").isEqualTo("/api/problems/not-found");
        json.extractingPath("$.detail").isEqualTo("Muskel 'calves' gibt es nicht.");
    }

    @Test
    void rejectsMalformedIds() {
        assertThat(mvc.get().uri("/api/v1/muscles/Side_Delts/exercises"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("/api/problems/invalid-parameter");
    }
}
