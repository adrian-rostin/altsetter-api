package de.adrianrostin.altsetter.mapper;

import de.adrianrostin.altsetter.dto.EquipmentDto;
import de.adrianrostin.altsetter.entity.Equipment;
import de.adrianrostin.altsetter.entity.Language;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EquipmentMapper {

    public EquipmentDto toDto(Equipment equipment, Language language) {
        return new EquipmentDto(equipment.getId(), equipment.name(language));
    }

    public List<EquipmentDto> toDtos(Collection<Equipment> equipment, Language language) {
        return equipment.stream().map(item -> toDto(item, language)).toList();
    }
}
