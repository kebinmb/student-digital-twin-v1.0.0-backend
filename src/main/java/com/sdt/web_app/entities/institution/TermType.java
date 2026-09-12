package com.sdt.web_app.entities.institution;

public enum TermType {
    FIRST_SEM("1ST_SEM"),
    SECOND_SEM("2ND_SEM"),
    SUMMER("SUMMER"),
    FIRST_SEMESTER("1ST_SEM"),
    SECOND_SEMESTER("2ND_SEM");

    private final String dbValue;

    TermType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TermType fromDbValue(String value) {
        if (value == null) return null;
        for (TermType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        if ("FIRST_SEMESTER".equalsIgnoreCase(value) || "1ST_SEM".equalsIgnoreCase(value)) {
            return FIRST_SEM;
        }
        if ("SECOND_SEMESTER".equalsIgnoreCase(value) || "2ND_SEM".equalsIgnoreCase(value)) {
            return SECOND_SEM;
        }
        throw new IllegalArgumentException("Unknown TermType value: " + value);
    }
}
