package de.adrianrostin.altsetter;

import static org.assertj.core.api.Assertions.assertThat;

import de.adrianrostin.altsetter.entity.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Jeder Text liegt in jeder Sprache vor. Die Datenbank kann das selbst nicht erzwingen; fehlt eine
 * Übersetzung, würde die API still auf Deutsch zurückfallen.
 */
@IntegrationTest
class TranslationsIT {

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {"muscle_family:family_id", "muscle:muscle_id", "equipment:equipment_id", "exercise:exercise_id"})
    void everyRowHasATextInEveryLanguage(String tableAndKey) {
        String table = tableAndKey.split(":")[0];
        String key = tableAndKey.split(":")[1];
        Integer missing = jdbc.queryForObject(
                ("select count(*) from altsetter.%s t cross join altsetter.language l where not exists "
                                + "(select 1 from altsetter.%s_text x where x.%s = t.id and x.language = l.code)")
                        .formatted(table, table, key),
                Integer.class);

        assertThat(missing).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Language.class)
    void everyLanguageHasTheSameTipsAsGerman(Language language) {
        Integer different = jdbc.queryForObject("""
                select count(*) from (
                    select exercise_id, kind, position from altsetter.exercise_tip where language = 'de'
                    except
                    select exercise_id, kind, position from altsetter.exercise_tip where language = ?
                ) missing
                """, Integer.class, language.getCode());

        assertThat(different).isZero();
    }
}
