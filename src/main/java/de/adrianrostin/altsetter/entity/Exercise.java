package de.adrianrostin.altsetter.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Eine Übung. Die Sammlungen werden bei Bedarf gebündelt nachgeladen
 * ({@code hibernate.default_batch_fetch_size}), so bleibt auch eine Liste bei wenigen Abfragen.
 */
@Entity
@Table(name = "exercise")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise {

    /** Kennung, z. B. „lat-pulldown“; in allen Sprachen gleich und auch in den Pfaden der App. */
    @Id
    private String id;

    private Difficulty difficulty;

    /** Name und Beschreibung je Sprachcode. */
    @ElementCollection
    @CollectionTable(name = "exercise_text", joinColumns = @JoinColumn(name = "exercise_id"))
    @MapKeyColumn(name = "language")
    private Map<String, ExerciseText> texts = new HashMap<>();

    @ManyToMany
    @JoinTable(
            name = "exercise_equipment",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    @OrderBy("sortOrder")
    private Set<Equipment> equipment = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "exercise_muscle", joinColumns = @JoinColumn(name = "exercise_id"))
    private Set<ExerciseMuscle> muscles = new LinkedHashSet<>();

    /** Hinweise in allen Sprachen. */
    @ElementCollection
    @CollectionTable(name = "exercise_tip", joinColumns = @JoinColumn(name = "exercise_id"))
    private Set<ExerciseTip> tips = new LinkedHashSet<>();

    public ExerciseText text(Language language) {
        return language.pick(texts);
    }

    /** IDs der Muskeln mit dieser Rolle, sortiert nach {@code order}. */
    public List<String> muscleIds(MuscleRole role, Comparator<String> order) {
        return muscles.stream()
                .filter(muscle -> muscle.role() == role)
                .map(ExerciseMuscle::muscleId)
                .sorted(order)
                .toList();
    }

    /**
     * Texte der Hinweise dieser Art in ihrer Reihenfolge und in der gewünschten Sprache. Hat die
     * Übung in dieser Sprache gar keine Hinweise, die der Standardsprache.
     */
    public List<String> tips(TipKind kind, Language language) {
        String code = tips.stream().anyMatch(tip -> tip.language().equals(language.getCode()))
                ? language.getCode()
                : Language.DEFAULT.getCode();
        return tips.stream()
                .filter(tip -> tip.kind() == kind && tip.language().equals(code))
                .sorted(Comparator.comparingInt(ExerciseTip::position))
                .map(ExerciseTip::text)
                .toList();
    }
}
