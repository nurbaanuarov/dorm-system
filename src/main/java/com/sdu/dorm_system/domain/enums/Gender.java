package com.sdu.dorm_system.domain.enums;

public enum Gender {
    MALE,
    FEMALE;

    public static Gender fromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "male", "boy", "boys", "m", "man" -> MALE;
            case "female", "girl", "girls", "f", "woman" -> FEMALE;
            default -> throw new IllegalArgumentException("Unsupported gender value: " + value);
        };
    }
}
