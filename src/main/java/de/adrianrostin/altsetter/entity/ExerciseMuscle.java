package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Ein Muskel, den eine Übung trainiert, mit seiner Rolle. */
@Embeddable
public record ExerciseMuscle(@Column(name = "muscle_id") String muscleId, MuscleRole role) {}
