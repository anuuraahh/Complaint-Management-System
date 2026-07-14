package com.examly.springapp.model;

import java.util.Arrays;

public enum ComplaintCategory {
    INFRASTRUCTURE,
    SERVICE,
    PERSONNEL,
    BILLING,
    OTHER;

    public static boolean isValid(String value) {
        if (value == null) return false;
        return Arrays.stream(values())
                .anyMatch(c -> c.name().equalsIgnoreCase(value));
    }
}