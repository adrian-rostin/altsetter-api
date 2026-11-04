package de.adrianrostin.altsetter.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sprachen, in denen die API Texte liefert. Clients wählen sie mit {@code Accept-Language}; ohne
 * passende Angabe gilt Deutsch. Der Code steht so in der Spalte {@code language} der Text-Tabellen.
 */
@Getter
@RequiredArgsConstructor
public enum Language implements CodedEnum {
    DE("de", Locale.GERMAN),
    EN("en", Locale.ENGLISH);

    public static final Language DEFAULT = DE;

    private final String code;
    private final Locale locale;

    /** Die Sprache zu einem Locale ({@code en-US} wird zu {@link #EN}); unbekannte zu {@link #DEFAULT}. */
    public static Language of(Locale locale) {
        return Arrays.stream(values())
                .filter(language -> language.locale.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(DEFAULT);
    }

    public static List<Locale> supportedLocales() {
        return Arrays.stream(values()).map(Language::getLocale).toList();
    }

    /**
     * Der Text in dieser Sprache aus {@code texts} (Schlüssel: Sprachcode), sonst der in der
     * Standardsprache. Fehlen beide, sind die Daten kaputt.
     */
    public <T> T pick(Map<String, T> texts) {
        T text = texts.get(code);
        if (text == null) {
            text = texts.get(DEFAULT.code);
        }
        if (text == null) {
            throw new IllegalStateException("Kein Text auf '%s' oder '%s'".formatted(code, DEFAULT.code));
        }
        return text;
    }
}
