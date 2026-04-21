package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.MealBooking;
import com.sdu.dorm_system.domain.enums.MealType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealBookingRepository extends JpaRepository<MealBooking, UUID> {

    boolean existsByStudentIdAndSlotDateAndMealType(UUID studentId, LocalDate slotDate, MealType mealType);

    long countBySlotId(UUID slotId);

    Optional<MealBooking> findByStudentIdAndSlotId(UUID studentId, UUID slotId);
}
