package de.adrianrostin.altsetter.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.service.ExerciseSearchService.Searchable;
import org.junit.jupiter.api.Test;

/** Die Trefferstufen ohne Datenbank. */
class ExerciseSearchServiceTest {

    private static final Searchable LANDMINE =
            searchable("Landmine Press", "Vordere Schulter Langhantel", "Landmine Press");
    private static final Searchable INCLINE_PRESS = searchable(
            "Schrägbankdrücken mit Kurzhanteln", "Obere Brust Kurzhantel Schrägbank", "Incline Dumbbell Press");
    private static final Searchable BARBELL_CURL = searchable("Langhantel-Curls", "Bizeps Langhantel", "Barbell Curl");

    @Test
    void ranksNamesStartingWithTheInputFirst() {
        assertThat(tier("Landm", LANDMINE)).isZero();
        assertThat(tier("l", LANDMINE)).isZero();
    }

    @Test
    void ignoresCaseUmlautsAndPunctuation() {
        assertThat(tier("SCHRAGBANK", INCLINE_PRESS)).isZero();
        assertThat(tier("schrägbankdrücken", INCLINE_PRESS)).isZero();
        assertThat(tier("langhantel curls", BARBELL_CURL)).isZero();
    }

    @Test
    void findsWordStartsAndPartsOfWords() {
        assertThat(tier("curls lang", BARBELL_CURL)).isEqualTo(1);
        assertThat(tier("kurzhanteln", INCLINE_PRESS)).isEqualTo(1);
        assertThat(tier("drücken", INCLINE_PRESS)).isEqualTo(2);
    }

    @Test
    void findsMainMusclesAndEquipmentBehindNames() {
        assertThat(tier("bizeps", BARBELL_CURL)).isEqualTo(3);
        assertThat(tier("obere brust", INCLINE_PRESS)).isEqualTo(3);
    }

    @Test
    void findsNamesInTheOtherLanguageLast() {
        assertThat(tier("bench", searchable("Bankdrücken", "Mittlere Brust", "Barbell Bench Press")))
                .isEqualTo(4);
        assertThat(tier("dumbbell press", INCLINE_PRESS)).isEqualTo(4);
    }

    @Test
    void needsEveryWordToMatch() {
        assertThat(tier("landmine curls", LANDMINE)).isEqualTo(Integer.MAX_VALUE);
        assertThat(tier("kniebeuge", LANDMINE)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void normalizesToPlainLowercaseWords() {
        assertThat(ExerciseSearchService.normalize("  Fuß-Übung  (Ärger) ")).isEqualTo("fuss ubung arger");
        assertThat(ExerciseSearchService.normalize("!!!")).isEmpty();
    }

    private static int tier(String query, Searchable exercise) {
        return ExerciseSearchService.tier(ExerciseSearchService.normalize(query), exercise);
    }

    private static Searchable searchable(String name, String extras, String otherNames) {
        return new Searchable(
                ExerciseSearchService.normalize(name),
                ExerciseSearchService.normalize(extras),
                ExerciseSearchService.normalize(otherNames));
    }
}
