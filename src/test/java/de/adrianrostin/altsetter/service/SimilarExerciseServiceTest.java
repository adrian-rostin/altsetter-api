package de.adrianrostin.altsetter.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.dto.ExerciseSummaryDto;
import de.adrianrostin.altsetter.entity.Difficulty;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Die Rangliste ohne Datenbank; Muskeln und Rollen wie im echten Katalog. */
class SimilarExerciseServiceTest {

    private static final ExerciseSummaryDto INCLINE_CABLE_FLYS =
            exercise("incline-cable-flys", List.of("upper-chest"), List.of("mid-chest", "front-delts"));

    @Test
    void putsExercisesWithTheSameMusclesFirst() {
        var inclineFlys = exercise("incline-flys", List.of("upper-chest"), List.of("mid-chest", "front-delts"));
        var inclinePress = exercise(
                "incline-press", List.of("upper-chest"), List.of("mid-chest", "front-delts", "triceps-lateral-head"));
        var lowToHigh = exercise("low-to-high-cable-flys", List.of("upper-chest"), List.of("front-delts"));

        var ranked = SimilarExerciseService.rank(
                INCLINE_CABLE_FLYS, List.of(inclinePress, lowToHigh, INCLINE_CABLE_FLYS, inclineFlys));

        assertThat(ranked)
                .extracting(ExerciseSummaryDto::id)
                .containsExactly("incline-flys", "incline-press", "low-to-high-cable-flys");
    }

    @Test
    void needsAMainMuscleInCommonBothWays() {
        // Trains the upper chest only along; its own main muscle (triceps) isn't trained by the flys.
        var dips = exercise("bench-dips", List.of("triceps-lateral-head"), List.of("upper-chest", "front-delts"));
        // Trains the front delts as main muscle, but not the upper chest at all.
        var frontRaises = exercise("front-raises", List.of("front-delts"), List.of("side-delts"));

        assertThat(SimilarExerciseService.rank(INCLINE_CABLE_FLYS, List.of(dips, frontRaises)))
                .isEmpty();
    }

    @Test
    void countsSwappedRolesOfTheSameMuscles() {
        var hammerCurls = exercise("hammer-curls", List.of("brachialis"), List.of("brachioradialis"));
        var ropeHammerCurls = exercise("rope-hammer-curls", List.of("brachioradialis"), List.of("brachialis"));

        assertThat(SimilarExerciseService.rank(hammerCurls, List.of(ropeHammerCurls)))
                .extracting(ExerciseSummaryDto::id)
                .containsExactly("rope-hammer-curls");
    }

    @Test
    void leavesOutWeakMatchesInsteadOfFillingUp() {
        // Shares the main muscle, but trains a lot besides it: similarity well below the threshold.
        var deadlift = exercise(
                "deadlift",
                List.of("hamstrings", "gluteus-maximus", "lower-back"),
                List.of("upper-traps", "lats", "forearm-flexors", "adductors", "vastus-lateralis"));
        var legCurl = exercise("leg-curl", List.of("hamstrings"), List.of("gastrocnemius"));

        assertThat(SimilarExerciseService.rank(legCurl, List.of(deadlift))).isEmpty();
    }

    @Test
    void returnsAtMostFiveAndKeepsTheNameOrderOnTies() {
        var candidates = List.of("a", "b", "c", "d", "e", "f", "g").stream()
                .map(id -> exercise(id, List.of("upper-chest"), List.of("mid-chest", "front-delts")))
                .toList();

        assertThat(SimilarExerciseService.rank(INCLINE_CABLE_FLYS, candidates))
                .extracting(ExerciseSummaryDto::id)
                .containsExactly("a", "b", "c", "d", "e");
    }

    private static ExerciseSummaryDto exercise(String id, List<String> primary, List<String> secondary) {
        return new ExerciseSummaryDto(id, id, "", Difficulty.BEGINNER, List.of(), primary, secondary, List.of());
    }
}
