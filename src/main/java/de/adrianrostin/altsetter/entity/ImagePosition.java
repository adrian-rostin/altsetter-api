package de.adrianrostin.altsetter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Welche Stellung ein Übungsbild zeigt. Halteübungen wie die Plank haben nur {@link #START}. */
@Getter
@RequiredArgsConstructor
public enum ImagePosition implements CodedEnum {
    /** Ausgangsstellung. */
    START("start"),
    /** Endstellung der Bewegung. */
    END("end");

    /** Für Pfade: {@code start} oder {@code end}. */
    public static final String PATTERN = "start|end";

    private final String code;
}
