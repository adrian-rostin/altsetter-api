package de.adrianrostin.altsetter.service;

import de.adrianrostin.altsetter.dto.MuscleFamilyDto;
import de.adrianrostin.altsetter.entity.Language;
import de.adrianrostin.altsetter.entity.Muscle;
import de.adrianrostin.altsetter.mapper.MuscleMapper;
import de.adrianrostin.altsetter.repository.MuscleFamilyRepository;
import de.adrianrostin.altsetter.repository.MuscleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Muskeln und Muskelgruppen. Der Katalog wird je Sprache einmal geladen und bleibt im Cache.
 * Aufrufer holen ihn immer über {@link #catalog}, denn nur Aufrufe von außen gehen durch den Cache.
 */
@Service
@RequiredArgsConstructor
public class MuscleService {

    public static final String CACHE = "muscle-catalog";

    private final MuscleRepository muscles;
    private final MuscleFamilyRepository families;
    private final MuscleMapper mapper;

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public MuscleCatalog catalog(Language language) {
        List<Muscle> allMuscles = muscles.findAllByOrderBySortOrder();
        List<MuscleFamilyDto> familyDtos = families.findAllByOrderBySortOrder().stream()
                .map(family -> mapper.toDto(family, allMuscles, language))
                .toList();
        return new MuscleCatalog(
                familyDtos,
                allMuscles.stream().map(muscle -> mapper.toDto(muscle, language)).toList());
    }
}
