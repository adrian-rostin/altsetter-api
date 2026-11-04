package de.adrianrostin.altsetter.entity;

import jakarta.persistence.AttributeConverter;

/** Speichert ein {@link CodedEnum} als seinen Code. Je Enum gibt es eine konkrete Unterklasse. */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected CodedEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(String code) {
        return code == null ? null : CodedEnum.fromCode(type, code);
    }
}
