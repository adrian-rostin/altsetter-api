package de.adrianrostin.altsetter.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Ein Muskel bzw. Muskelanteil, so wie ihn das 3D-Modell der App kennt. */
@Entity
@Table(name = "muscle")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Muscle {

    /** Kennung, z. B. „upper-chest“; in allen Sprachen gleich und auch in den Pfaden der App. */
    @Id
    private String id;

    /** Muskelgruppe, z. B. „chest“; {@code null} bei eigenständigen Muskeln. */
    private String familyId;

    private String latinName;

    private short sortOrder;

    /** Texte je Sprachcode. */
    @ElementCollection
    @CollectionTable(name = "muscle_text", joinColumns = @JoinColumn(name = "muscle_id"))
    @MapKeyColumn(name = "language")
    private Map<String, MuscleText> texts = new HashMap<>();

    /** Mesh-Namen im 3D-Modell ohne Seitenangabe, z. B. „Long head of biceps brachii“. */
    @ElementCollection
    @CollectionTable(name = "muscle_mesh", joinColumns = @JoinColumn(name = "muscle_id"))
    @Column(name = "mesh_name")
    @OrderBy
    private Set<String> meshNames = new LinkedHashSet<>();

    public MuscleText text(Language language) {
        return language.pick(texts);
    }
}
