package com.nexus.nexus.Enumaration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TypeOfReport{
    LOST,
    FOUND;

    @JsonCreator
    public static TypeOfReport fromString(String value) {
        if (value == null) {
            return null;
        }
        return TypeOfReport.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
