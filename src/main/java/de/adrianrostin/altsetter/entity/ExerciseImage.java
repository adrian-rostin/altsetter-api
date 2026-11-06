package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Foto einer Übung in Start- oder Endstellung, als Datei in der Datenbank (WebP, 1536 × 1024). Die
 * Bytes werden nur für den Bild-Endpunkt gelesen; Listen holen per {@code ExerciseImageRepository}
 * nur Größe und Prüfsumme.
 */
@Entity
@Table(name = "exercise_image")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseImage {

    @EmbeddedId
    private Key key;

    /** z. B. {@code image/webp} */
    private String mimeType;

    private int width;

    private int height;

    private byte[] data;

    /**
     * @param position Code aus {@link ImagePosition}; im Schlüssel als Text, weil JPA auf IDs keine
     *     Konverter anwendet
     */
    @Embeddable
    public record Key(@Column(name = "exercise_id") String exerciseId, String position) implements Serializable {}
}
