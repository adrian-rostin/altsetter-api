package de.adrianrostin.altsetter.dto;

import de.adrianrostin.altsetter.entity.ImagePosition;

/**
 * Ein Foto einer Übung.
 *
 * @param position Start- oder Endstellung
 * @param url Pfad zum Bild auf derselben Domain, z. B.
 *     {@code /api/v1/exercises/lat-pulldown/images/start?v=…}. {@code v} ändert sich mit dem Bild, deshalb
 *     darf der Browser es unbegrenzt behalten.
 */
public record ExerciseImageDto(ImagePosition position, String url, int width, int height) {}
