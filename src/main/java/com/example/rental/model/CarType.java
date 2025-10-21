package com.example.rental.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CarType {
    SEDAN("sedan"),
    SUV("suv"),
    VAN("van");

    private final String code;

    CarType(String code) { this.code = code; }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static CarType from(String value) {
        if (value == null) throw new IllegalArgumentException("carType is required");
        String v = value.trim();
        for (CarType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.code.equalsIgnoreCase(v)) return t;
        }
        throw new IllegalArgumentException("Unknown carType: " + value + " (allowed: sedan, suv, van)");
    }
}
