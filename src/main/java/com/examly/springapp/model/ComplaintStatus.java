package com.examly.springapp.model;

import java.util.Arrays;

public enum ComplaintStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public static boolean isValid(String value) {
        if (value == null) return false;
        return Arrays.stream(values())
                .anyMatch(s -> s.name().equalsIgnoreCase(value));
    }
}