package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.MuscleDto;
import de.adrianrostin.altsetter.dto.MuscleFamilyDto;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unveränderlicher Schnappschuss aller Muskeln und Muskelgruppen in fester Reihenfolge. Die Daten
 * ändern sich selten und nur von Hand in der Datenbank (siehe README, „Daten“).
 */
public final class MuscleCatalog {

    private final List<MuscleFamilyDto> families;
    private final List<MuscleDto> muscles;
    private final Map<String, MuscleDto> musclesById;
    private final Comparator<String> muscleOrder;

    public MuscleCatalog(List<MuscleFamilyDto> families, List<MuscleDto> muscles) {
        this.families = List.copyOf(families);
        this.muscles = List.copyOf(muscles);
        this.musclesById = this.muscles.stream().collect(Collectors.toMap(MuscleDto::id, Function.identity()));

        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < this.muscles.size(); i++) {
            positions.put(this.muscles.get(i).id(), i);
        }
        this.muscleOrder = Comparator.comparingInt(id -> positions.getOrDefault(id, Integer.MAX_VALUE));
    }

    public List<MuscleFamilyDto> families() {
        return families;
    }

    public List<MuscleDto> muscles() {
        return muscles;
    }

    /** Ein Muskel, auf den eine Übung verweist. Fremdschlüssel garantieren, dass es ihn gibt. */
    public MuscleDto muscle(String id) {
        MuscleDto muscle = musclesById.get(id);
        if (muscle == null) {
            throw new IllegalStateException("Muskel '%s' fehlt im Katalog".formatted(id));
        }
        return muscle;
    }

    /** Sortiert Muskel-IDs so wie in der App (von oben nach unten am Körper); unbekannte ans Ende. */
    public Comparator<String> muscleOrder() {
        return muscleOrder;
    }
}
