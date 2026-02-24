package com.nexus.nexus.Enumaration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    OPEN,
    MATCHED,
    DELIVERED,
    ACTIVE;

    @JsonCreator
    public static Status fromString(String value) {
        if (value == null) {
            return null;
        }
        return Status.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
