package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Embeddable;

/** Texte eines Muskels oder einer Muskelgruppe in einer Sprache. */
@Embeddable
public record MuscleText(String name, String region, String summary) {}
