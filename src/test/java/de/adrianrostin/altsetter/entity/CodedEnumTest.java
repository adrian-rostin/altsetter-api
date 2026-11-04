package de.adrianrostin.altsetter.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CodedEnumTest {

    @ParameterizedTest
    @ValueSource(strings = {"beginner", "BEGINNER", "Beginner"})
    void findsConstantsIgnoringCase(String code) {
        assertThat(CodedEnum.fromCode(Difficulty.class, code)).isEqualTo(Difficulty.BEGINNER);
    }

    @Test
    void rejectsUnknownCodes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CodedEnum.fromCode(Difficulty.class, "extrem"))
                .withMessage("Unbekannter Wert 'extrem'");
    }
}
