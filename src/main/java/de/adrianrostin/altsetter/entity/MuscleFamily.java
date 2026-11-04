package de.adrianrostin.altsetter.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Eine Muskelgruppe aus Anteilen, die unterschiedlich trainiert werden (z. B. die Schulter). */
@Entity
@Table(name = "muscle_family")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MuscleFamily {

    /** Kennung, z. B. „shoulders“; in allen Sprachen gleich. */
    @Id
    private String id;

    private String latinName;

    private short sortOrder;

    /** Texte je Sprachcode. */
    @ElementCollection
    @CollectionTable(name = "muscle_family_text", joinColumns = @JoinColumn(name = "family_id"))
    @MapKeyColumn(name = "language")
    private Map<String, MuscleText> texts = new HashMap<>();

    public MuscleText text(Language language) {
        return language.pick(texts);
    }
}
