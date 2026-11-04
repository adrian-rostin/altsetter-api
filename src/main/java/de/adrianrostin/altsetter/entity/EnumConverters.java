package de.adrianrostin.altsetter.entity;

import jakarta.persistence.Converter;

/** JPA-Konverter für die Enums dieses Pakets; Hibernate wendet sie automatisch an. */
final class EnumConverters {

    private EnumConverters() {}

    @Converter(autoApply = true)
    static class DifficultyConverter extends CodedEnumConverter<Difficulty> {
        DifficultyConverter() {
            super(Difficulty.class);
        }
    }

    @Converter(autoApply = true)
    static class MuscleRoleConverter extends CodedEnumConverter<MuscleRole> {
        MuscleRoleConverter() {
            super(MuscleRole.class);
        }
    }

    @Converter(autoApply = true)
    static class TipKindConverter extends CodedEnumConverter<TipKind> {
        TipKindConverter() {
            super(TipKind.class);
        }
    }
}
