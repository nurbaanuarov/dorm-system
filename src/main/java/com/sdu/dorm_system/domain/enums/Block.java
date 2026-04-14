package com.sdu.dorm_system.domain.enums;

public enum Block {
    A,
    B,
    C,
    D;

    public boolean supportsGender(Gender gender) {
        return switch (this) {
            case A, B -> gender == Gender.FEMALE;
            case C, D -> gender == Gender.MALE;
        };
    }
}
