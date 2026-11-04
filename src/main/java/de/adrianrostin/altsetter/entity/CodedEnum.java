package de.adrianrostin.altsetter.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/**
 * Enum mit einem festen Code, der so in der Datenbank, in der API und in Query-Parametern steht
 * (z. B. {@code "beginner"}). Entkoppelt den Code vom Namen der Konstante.
 */
public interface CodedEnum {

    @JsonValue
    String getCode();

    /** Sucht die Konstante zu einem Code, ohne auf Groß- und Kleinschreibung zu achten. */
    static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, String code) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Wert '%s'".formatted(code)));
    }
}
