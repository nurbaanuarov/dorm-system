package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.StudentMealPlan;
import com.sdu.dorm_system.domain.enums.MealType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentMealPlanRepository extends JpaRepository<StudentMealPlan, UUID> {

    List<StudentMealPlan> findAllByStudentIdOrderByMealTypeAsc(UUID studentId);

    boolean existsByStudentIdAndMealType(UUID studentId, MealType mealType);

    void deleteAllByStudentId(UUID studentId);
}
