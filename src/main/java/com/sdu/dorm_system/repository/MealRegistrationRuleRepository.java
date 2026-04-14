package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.MealRegistrationRule;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.MealType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRegistrationRuleRepository extends JpaRepository<MealRegistrationRule, UUID> {

    Optional<MealRegistrationRule> findByGenderScopeAndMealTypeAndRegistrationDate(
        Gender genderScope,
        MealType mealType,
        LocalDate registrationDate
    );

    List<MealRegistrationRule> findAllByGenderScopeAndRegistrationDateOrderByMealTypeAsc(
        Gender genderScope,
        LocalDate registrationDate
    );
}
