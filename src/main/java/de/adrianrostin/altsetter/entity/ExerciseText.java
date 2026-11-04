package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Embeddable;

/** Name und Kurzbeschreibung einer Übung in einer Sprache. */
@Embeddable
public record ExerciseText(String name, String description) {}
