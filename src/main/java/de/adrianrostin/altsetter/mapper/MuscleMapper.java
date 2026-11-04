package de.adrianrostin.altsetter.mapper;

import de.adrianrostin.altsetter.dto.MuscleDto;
import de.adrianrostin.altsetter.dto.MuscleFamilyDto;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.entity.Muscle;
import de.adrianrostin.altsetter.entity.MuscleFamily;
import de.adrianrostin.altsetter.entity.MuscleText;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MuscleMapper {

    public MuscleDto toDto(Muscle muscle, Language language) {
        MuscleText text = muscle.text(language);
        return new MuscleDto(
                muscle.getId(),
                muscle.getFamilyId(),
                text.name(),
                muscle.getLatinName(),
                text.region(),
                text.summary(),
                List.copyOf(muscle.getMeshNames()));
    }

    /** {@code allMuscles} in Körperreihenfolge; daraus ergeben sich die Anteile der Gruppe. */
    public MuscleFamilyDto toDto(MuscleFamily family, List<Muscle> allMuscles, Language language) {
        List<String> parts = allMuscles.stream()
                .filter(muscle -> family.getId().equals(muscle.getFamilyId()))
                .map(Muscle::getId)
                .toList();
        MuscleText text = family.text(language);
        return new MuscleFamilyDto(
                family.getId(), text.name(), family.getLatinName(), text.region(), text.summary(), parts);
    }
}
