package com.sdt.web_app.entities.institution;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TermTypeConverter implements AttributeConverter<TermType, String> {

    @Override
    public String convertToDatabaseColumn(TermType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public TermType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TermType.fromDbValue(dbData);
    }
}
