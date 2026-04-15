package com.sdu.dorm_system.domain;

import com.sdu.dorm_system.domain.enums.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dorm_registration_meal_option")
public class DormRegistrationMealOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settings_id", nullable = false)
    private DormRegistrationSettings settings;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "included_in_price", nullable = false)
    private boolean includedInPrice;
}
