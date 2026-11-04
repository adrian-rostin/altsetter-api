package de.adrianrostin.altsetter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class ExerciseControllerIT {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    class Listing {

        @Test
        void pagesThroughAllExercisesSortedByName() {
            var json = assertThat(mvc.get().uri("/api/v1/exercises?size=2"))
                    .hasStatusOk()
                    .bodyJson();

            json.extractingPath("$.items[*].id")
                    .asArray()
                    .containsExactly("barbell-bench-press", "reverse-dumbbell-fly");
            json.extractingPath("$.totalItems").isEqualTo(3);
            json.extractingPath("$.totalPages").isEqualTo(2);
        }

        @Test
        void returnsSummariesWithMusclesInBodyOrderAndEquipment() {
            var json = assertThat(mvc.get().uri("/api/v1/exercises?size=100"))
                    .hasStatusOk()
                    .bodyJson();

            json.extractingPath("$.items[?(@.id == 'barbell-bench-press')].difficulty")
                    .asArray()
                    .containsExactly("intermediate");
            json.extractingPath("$.items[?(@.id == 'barbell-bench-press')].equipment[*].id")
                    .asArray()
                    .containsExactly("barbell", "flat-bench");
            json.extractingPath("$.items[?(@.id == 'barbell-bench-press')].secondaryMuscles[*]")
                    .asArray()
                    .containsExactly("upper-chest", "lower-chest", "front-delts");
        }

        @Test
        void sortsByTheEnglishNamesWhenAskedForEnglish() {
            var result = assertThat(
                    mvc.get().uri("/api/v1/exercises?size=2").header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9"));

            result.hasStatusOk().headers().hasValue(HttpHeaders.CONTENT_LANGUAGE, "en");
            result.bodyJson()
                    .extractingPath("$.items[*].id")
                    .asArray()
                    .containsExactly("barbell-bench-press", "dumbbell-lateral-raise");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"size=0", "size=101", "page=-1", "page=eins"})
        void rejectsInvalidPaging(String query) {
            var result = assertThat(mvc.get().uri("/api/v1/exercises?" + query));

            result.hasStatus(HttpStatus.BAD_REQUEST);
            result.bodyJson().extractingPath("$.type").isEqualTo("/api/problems/invalid-parameter");
        }
    }

    @Nested
    class Detail {

        @Test
        void containsTipsInOrderAndMusclesByBodyPosition() {
            var json = assertThat(mvc.get().uri("/api/v1/exercises/barbell-bench-press"))
                    .hasStatusOk()
                    .bodyJson();

            json.extractingPath("$.name").isEqualTo("Bankdrücken mit der Langhantel");
            json.extractingPath("$.cues[0]").isEqualTo("Füße fest am Boden.");
            json.extractingPath("$.cues.length()").isEqualTo(2);
            json.extractingPath("$.mistakes.length()").isEqualTo(1);
            json.extractingPath("$.muscles.primary[0].name").isEqualTo("Mittlere Brust");
            json.extractingPath("$.muscles.secondary[*].id")
                    .asArray()
                    .containsExactly("upper-chest", "lower-chest", "front-delts");
        }

        @Test
        void isTranslatedCompletelyIntoEnglish() {
            var result = assertThat(
                    mvc.get().uri("/api/v1/exercises/barbell-bench-press").header(HttpHeaders.ACCEPT_LANGUAGE, "en"));

            result.headers().hasValue(HttpHeaders.CONTENT_LANGUAGE, "en");
            var json = result.hasStatusOk().bodyJson();
            json.extractingPath("$.name").isEqualTo("Barbell Bench Press");
            json.extractingPath("$.equipment[0].name").isEqualTo("Barbell");
            json.extractingPath("$.muscles.primary[0].name").isEqualTo("Mid Chest");
            json.extractingPath("$.cues[0]").isEqualTo("Feet planted.");
            json.extractingPath("$.cues.length()").isEqualTo(2);
            json.extractingPath("$.mistakes.length()").isEqualTo(1);
        }

        @Test
        void fallsBackToGermanForOtherLanguages() {
            var result = assertThat(mvc.get()
                    .uri("/api/v1/exercises/barbell-bench-press")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"));

            result.headers().hasValue(HttpHeaders.CONTENT_LANGUAGE, "de");
            result.hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Bankdrücken mit der Langhantel");
        }

        @Test
        void answersUnknownExercisesInTheRequestedLanguage() {
            assertThat(mvc.get().uri("/api/v1/exercises/handstand-deadlift").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.detail")
                    .isEqualTo("Exercise 'handstand-deadlift' does not exist.");
        }

        @Test
        void answersUnknownExercisesWith404() {
            var result = assertThat(mvc.get().uri("/api/v1/exercises/handstand-deadlift"));

            result.hasStatus(HttpStatus.NOT_FOUND).headers().hasValue("Cache-Control", "no-store");
            var json = result.bodyJson();
            json.extractingPath("$.type").isEqualTo("/api/problems/not-found");
            json.extractingPath("$.detail").isEqualTo("Übung 'handstand-deadlift' gibt es nicht.");
            json.extractingPath("$.requestId").asString().isNotBlank();
        }

        @Test
        void rejectsMalformedIds() {
            assertThat(mvc.get().uri("/api/v1/exercises/Barbell_Bench_Press"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.type")
                    .isEqualTo("/api/problems/invalid-parameter");
        }
    }
}
