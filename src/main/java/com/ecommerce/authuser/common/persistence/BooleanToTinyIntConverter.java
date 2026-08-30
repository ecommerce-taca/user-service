package com.ecommerce.authuser.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

@Convert
public class BooleanToTinyIntConverter implements AttributeConverter<Boolean, Byte> {

    @Override
    public Byte convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute ? (byte) 1 : (byte) 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Byte dbData) {
        if (dbData == null) {
            return null;
        }

        return dbData == 1;
    }
}
