package de.adrianrostin.altsetter;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kleiner Testkatalog für die Integrationstests, nur im Test-Container: zwei Muskelgruppen mit
 * ihren Teilen, drei Geräte und drei Übungen, jeweils auf Deutsch und Englisch.
 *
 * <p>Die Übungen heißen so, dass sie auf Deutsch und Englisch unterschiedlich sortiert sind:
 * Deutsch Bankdrücken, Reverse Flys, Seitheben; Englisch Barbell Bench Press, Dumbbell Lateral Raise,
 * Reverse Dumbbell Fly.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestCatalog {

    @Bean
    ApplicationRunner fillTestCatalog(JdbcTemplate jdbc) {
        return args -> {
            family(jdbc, "chest", "Pectoralis major", 1, "Brust", "Chest");
            family(jdbc, "shoulders", "Deltoideus", 2, "Schulter", "Shoulders");

            muscle(jdbc, "upper-chest", "chest", 1, "Obere Brust", "Upper Chest");
            muscle(jdbc, "mid-chest", "chest", 2, "Mittlere Brust", "Mid Chest");
            muscle(jdbc, "lower-chest", "chest", 3, "Untere Brust", "Lower Chest");
            muscle(jdbc, "front-delts", "shoulders", 4, "Vordere Schulter", "Front Delts");
            muscle(jdbc, "side-delts", "shoulders", 5, "Seitliche Schulter", "Side Delts");
            muscle(jdbc, "rear-delts", "shoulders", 6, "Hintere Schulter", "Rear Delts");

            equipment(jdbc, "barbell", 1, "Langhantel", "Barbell");
            equipment(jdbc, "dumbbell", 2, "Kurzhantel", "Dumbbell");
            equipment(jdbc, "flat-bench", 3, "Flachbank", "Flat Bench");

            exercise(
                    jdbc,
                    "barbell-bench-press",
                    "intermediate",
                    "Bankdrücken mit der Langhantel",
                    "Barbell Bench Press");
            uses(jdbc, "barbell-bench-press", "flat-bench", "barbell");
            trains(jdbc, "barbell-bench-press", "primary", "mid-chest");
            // Absichtlich nicht in Körperreihenfolge: die API sortiert selbst.
            trains(jdbc, "barbell-bench-press", "secondary", "front-delts", "upper-chest", "lower-chest");
            tips(jdbc, "barbell-bench-press", "de", "cue", "Füße fest am Boden.", "Schulterblätter zusammen.");
            tips(jdbc, "barbell-bench-press", "de", "mistake", "Po hebt von der Bank ab.");
            tips(jdbc, "barbell-bench-press", "en", "cue", "Feet planted.", "Shoulder blades together.");
            tips(jdbc, "barbell-bench-press", "en", "mistake", "Hips lift off the bench.");
            // Absichtlich Ende vor Start: die API sortiert selbst.
            image(jdbc, "barbell-bench-press", "end", END_IMAGE);
            image(jdbc, "barbell-bench-press", "start", START_IMAGE);

            exercise(jdbc, "dumbbell-lateral-raise", "beginner", "Seitheben mit Kurzhanteln", "Dumbbell Lateral Raise");
            uses(jdbc, "dumbbell-lateral-raise", "dumbbell");
            trains(jdbc, "dumbbell-lateral-raise", "primary", "side-delts");
            // Wie eine Halteübung: nur ein Startbild.
            image(jdbc, "dumbbell-lateral-raise", "start", START_IMAGE);

            exercise(jdbc, "reverse-dumbbell-fly", "beginner", "Reverse Flys mit Kurzhanteln", "Reverse Dumbbell Fly");
            uses(jdbc, "reverse-dumbbell-fly", "dumbbell");
            trains(jdbc, "reverse-dumbbell-fly", "primary", "rear-delts");
        };
    }

    /** Stellvertreter für Bilddateien; der Inhalt ist der API egal. */
    public static final byte[] START_IMAGE = {'R', 'I', 'F', 'F', 1, 2, 3};

    public static final byte[] END_IMAGE = {'R', 'I', 'F', 'F', 4, 5, 6};

    private static void image(JdbcTemplate jdbc, String exercise, String position, byte[] data) {
        jdbc.update(
                "insert into altsetter.exercise_image (exercise_id, position, mime_type, width, height, data)"
                        + " values (?, ?, 'image/webp', 1536, 1024, ?)",
                exercise,
                position,
                data);
    }

    private static void family(JdbcTemplate jdbc, String id, String latin, int order, String de, String en) {
        jdbc.update(
                "insert into altsetter.muscle_family (id, latin_name, sort_order) values (?, ?, ?)", id, latin, order);
        for (String[] text : new String[][] {{"de", de}, {"en", en}}) {
            jdbc.update(
                    "insert into altsetter.muscle_family_text (family_id, language, name, region, summary)"
                            + " values (?, ?, ?, ?, ?)",
                    id,
                    text[0],
                    text[1],
                    text[1],
                    "Summary " + text[1]);
        }
    }

    private static void muscle(JdbcTemplate jdbc, String id, String family, int order, String de, String en) {
        jdbc.update(
                "insert into altsetter.muscle (id, family_id, latin_name, sort_order) values (?, ?, ?, ?)",
                id,
                family,
                "Latin " + id,
                order);
        for (String[] text : new String[][] {{"de", de}, {"en", en}}) {
            jdbc.update(
                    "insert into altsetter.muscle_text (muscle_id, language, name, region, summary)"
                            + " values (?, ?, ?, ?, ?)",
                    id,
                    text[0],
                    text[1],
                    "Region",
                    "Summary " + text[1]);
        }
        jdbc.update("insert into altsetter.muscle_mesh (muscle_id, mesh_name) values (?, ?)", id, "Mesh " + id);
    }

    private static void equipment(JdbcTemplate jdbc, String id, int order, String de, String en) {
        jdbc.update("insert into altsetter.equipment (id, sort_order) values (?, ?)", id, order);
        jdbc.update("insert into altsetter.equipment_text (equipment_id, language, name) values (?, 'de', ?)", id, de);
        jdbc.update("insert into altsetter.equipment_text (equipment_id, language, name) values (?, 'en', ?)", id, en);
    }

    private static void exercise(JdbcTemplate jdbc, String id, String difficulty, String de, String en) {
        jdbc.update("insert into altsetter.exercise (id, difficulty) values (?, ?)", id, difficulty);
        for (String[] text : new String[][] {{"de", de}, {"en", en}}) {
            jdbc.update(
                    "insert into altsetter.exercise_text (exercise_id, language, name, description)"
                            + " values (?, ?, ?, ?)",
                    id,
                    text[0],
                    text[1],
                    "Description " + text[1]);
        }
    }

    private static void uses(JdbcTemplate jdbc, String exercise, String... equipment) {
        for (String item : equipment) {
            jdbc.update(
                    "insert into altsetter.exercise_equipment (exercise_id, equipment_id) values (?, ?)",
                    exercise,
                    item);
        }
    }

    private static void trains(JdbcTemplate jdbc, String exercise, String role, String... muscles) {
        for (String muscle : muscles) {
            jdbc.update(
                    "insert into altsetter.exercise_muscle (exercise_id, muscle_id, role) values (?, ?, ?)",
                    exercise,
                    muscle,
                    role);
        }
    }

    private static void tips(JdbcTemplate jdbc, String exercise, String language, String kind, String... texts) {
        for (int i = 0; i < texts.length; i++) {
            jdbc.update(
                    "insert into altsetter.exercise_tip (exercise_id, language, kind, position, text)"
                            + " values (?, ?, ?, ?, ?)",
                    exercise,
                    language,
                    kind,
                    i + 1,
                    texts[i]);
        }
    }
}
