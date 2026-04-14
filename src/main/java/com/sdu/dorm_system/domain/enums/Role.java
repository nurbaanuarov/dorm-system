package com.sdu.dorm_system.domain.enums;

public enum Role {
    LEAD_ADMIN,
    BOYS_ADMIN,
    GIRLS_ADMIN,
    STUDENT;

    public boolean isGenderAdmin() {
        return this == BOYS_ADMIN || this == GIRLS_ADMIN;
    }

    public Gender managedGender() {
        return switch (this) {
            case BOYS_ADMIN -> Gender.MALE;
            case GIRLS_ADMIN -> Gender.FEMALE;
            default -> null;
        };
    }
}
