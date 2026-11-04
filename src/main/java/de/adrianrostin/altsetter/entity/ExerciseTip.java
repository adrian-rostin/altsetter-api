package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Embeddable;

/**
 * Ein Hinweis zur Ausführung: worauf man achten soll oder ein häufiger Fehler.
 *
 * @param language Sprachcode, siehe {@link Language}
 */
@Embeddable
public record ExerciseTip(String language, TipKind kind, short position, String text) {}
