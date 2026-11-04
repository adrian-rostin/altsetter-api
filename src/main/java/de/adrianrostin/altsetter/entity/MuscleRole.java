package de.adrianrostin.altsetter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Welche Rolle ein Muskel bei einer Übung spielt. */
@Getter
@RequiredArgsConstructor
public enum MuscleRole implements CodedEnum {
    /** Hauptmuskel: das, was die Übung trainieren soll. */
    PRIMARY("primary"),
    /** Arbeitet mit, wird aber nicht gezielt trainiert. */
    SECONDARY("secondary");

    private final String code;
}
