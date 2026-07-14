package com.examly.springapp.model;

import java.util.Arrays;

public enum ComplaintPriority {
    LOW,
    MEDIUM,
    HIGH;

    public static boolean isValid(String value) {
        if (value == null) return false;
        return Arrays.stream(values())
                .anyMatch(p -> p.name().equalsIgnoreCase(value));
    }
}