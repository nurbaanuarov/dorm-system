package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.MealSlot;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.MealType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealSlotRepository extends JpaRepository<MealSlot, UUID> {

    List<MealSlot> findAllByGenderScopeAndMealTypeAndSlotDateOrderByStartTimeAsc(
        Gender genderScope,
        MealType mealType,
        LocalDate slotDate
    );
}
