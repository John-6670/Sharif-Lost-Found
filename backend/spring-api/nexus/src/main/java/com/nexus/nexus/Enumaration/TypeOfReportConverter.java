package com.nexus.nexus.Enumaration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TypeOfReportConverter implements AttributeConverter<TypeOfReport, String> {

    @Override
    public String convertToDatabaseColumn(TypeOfReport attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public TypeOfReport convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return TypeOfReport.valueOf(dbData.trim().toUpperCase());
    }
}
