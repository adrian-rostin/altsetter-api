package de.adrianrostin.altsetter.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

/** Equipment, das eine Übung braucht, z. B. Kurzhantel oder Kabelzug. */
@Entity
@Table(name = "equipment")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment {

    /** Kennung, z. B. „dumbbell“; in allen Sprachen gleich. */
    @Id
    private String id;

    private short sortOrder;

    /** Namen je Sprachcode. */
    @ElementCollection
    @CollectionTable(name = "equipment_text", joinColumns = @JoinColumn(name = "equipment_id"))
    @MapKeyColumn(name = "language")
    @Column(name = "name")
    private Map<String, String> names = new HashMap<>();

    public String name(Language language) {
        return language.pick(names);
    }
}
