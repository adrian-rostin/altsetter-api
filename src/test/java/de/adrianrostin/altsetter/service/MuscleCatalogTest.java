package de.adrianrostin.altsetter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.adrianrostin.altsetter.dto.MuscleDto;
import de.adrianrostin.altsetter.dto.MuscleFamilyDto;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MuscleCatalogTest {

    private final MuscleCatalog catalog = new MuscleCatalog(
            List.of(new MuscleFamilyDto(
                    "shoulders", "Schulter", "Deltoideus", "Schultern", "", List.of("front-delts"))),
            List.of(muscle("upper-chest"), muscle("front-delts"), muscle("biceps-long-head")));

    @Test
    void sortsMuscleIdsByBodyPositionAndUnknownOnesLast() {
        List<String> sorted = Stream.of("unknown", "biceps-long-head", "upper-chest", "front-delts")
                .sorted(catalog.muscleOrder())
                .toList();

        assertThat(sorted).containsExactly("upper-chest", "front-delts", "biceps-long-head", "unknown");
    }

    @Test
    void findsMusclesById() {
        assertThat(catalog.muscle("front-delts").name()).isEqualTo("Name front-delts");
        assertThat(catalog.families()).extracting(MuscleFamilyDto::id).containsExactly("shoulders");
    }

    @Test
    void treatsAMissingMuscleAsBrokenData() {
        assertThatThrownBy(() -> catalog.muscle("calves"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Muskel 'calves' fehlt im Katalog");
    }

    private static MuscleDto muscle(String id) {
        return new MuscleDto(id, null, "Name " + id, "", "", "", List.of());
    }
}
