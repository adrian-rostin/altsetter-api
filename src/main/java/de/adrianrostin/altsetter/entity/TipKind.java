package de.adrianrostin.altsetter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipKind implements CodedEnum {
    /** „Darauf achten“. */
    CUE("cue"),
    /** „Häufige Fehler“. */
    MISTAKE("mistake");

    private final String code;
}
