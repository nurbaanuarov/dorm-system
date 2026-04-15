package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.MealRegistrationRule;
import com.sdu.dorm_system.domain.StudentMealPlan;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.MealRegistrationRuleRepository;
import com.sdu.dorm_system.repository.StudentMealPlanRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealRegistrationRuleRepository mealRegistrationRuleRepository;
    private final StudentMealPlanRepository studentMealPlanRepository;

    @Transactional
    public MealRegistrationRuleView upsertRegistrationRule(UserAccount admin, UpsertMealRegistrationRuleCommand command) {
        Gender managedGender = resolveManagedGender(admin);

        MealRegistrationRule rule = mealRegistrationRuleRepository
            .findByGenderScopeAndMealTypeAndRegistrationDate(managedGender, command.mealType(), command.registrationDate())
            .orElseGet(() -> {
                MealRegistrationRule newRule = new MealRegistrationRule();
                newRule.setMealType(command.mealType());
                newRule.setGenderScope(managedGender);
                newRule.setRegistrationDate(command.registrationDate());
                newRule.setCreatedBy(admin);
                return newRule;
            });

        rule.setActive(command.active());
        MealRegistrationRule savedRule = mealRegistrationRuleRepository.save(rule);
        return toView(savedRule);
    }

    @Transactional(readOnly = true)
    public List<MealRegistrationRuleView> listRegistrationRulesForAdmin(UserAccount admin, LocalDate registrationDate) {
        return listRegistrationRules(resolveManagedGender(admin), registrationDate);
    }

    @Transactional(readOnly = true)
    public List<MealRegistrationRuleView> listRegistrationRulesForStudent(UserAccount student, LocalDate registrationDate) {
        requireStudent(student);
        return listRegistrationRules(requireStudentGender(student), registrationDate);
    }

    @Transactional
    public List<MealType> replaceStudentMealPlan(UserAccount student, List<MealType> selectedMealTypes) {
        requireStudent(student);

        Set<MealType> normalizedMealTypes = selectedMealTypes == null
            ? Set.of()
            : new LinkedHashSet<>(selectedMealTypes);

        studentMealPlanRepository.deleteAllByStudentId(student.getId());

        List<StudentMealPlan> mealPlans = normalizedMealTypes.stream()
            .map(mealType -> {
                StudentMealPlan mealPlan = new StudentMealPlan();
                mealPlan.setStudent(student);
                mealPlan.setMealType(mealType);
                return mealPlan;
            })
            .toList();

        if (!mealPlans.isEmpty()) {
            studentMealPlanRepository.saveAll(mealPlans);
        }

        return getSelectedMealTypes(student);
    }

    @Transactional(readOnly = true)
    public List<MealType> getSelectedMealTypes(UserAccount student) {
        return studentMealPlanRepository.findAllByStudentIdOrderByMealTypeAsc(student.getId())
            .stream()
            .map(StudentMealPlan::getMealType)
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean isMealTypeIncluded(UserAccount student, MealType mealType) {
        requireStudent(student);
        return studentMealPlanRepository.existsByStudentIdAndMealType(student.getId(), mealType);
    }

    @Transactional(readOnly = true)
    public void ensureMealTypeIncluded(UserAccount student, MealType mealType) {
        if (!isMealTypeIncluded(student, mealType)) {
            throw BusinessException.conflict("This meal type is not included in the student's dorm price plan");
        }
    }

    private List<MealRegistrationRuleView> listRegistrationRules(Gender gender, LocalDate registrationDate) {
        List<MealRegistrationRule> existingRules = mealRegistrationRuleRepository
            .findAllByGenderScopeAndRegistrationDateOrderByMealTypeAsc(gender, registrationDate);

        return Arrays.stream(MealType.values())
            .map(mealType -> existingRules.stream()
                .filter(rule -> rule.getMealType() == mealType)
                .findFirst()
                .map(this::toView)
                .orElse(new MealRegistrationRuleView(mealType, gender, registrationDate, true)))
            .toList();
    }

    private Gender resolveManagedGender(UserAccount admin) {
        if (!admin.getRole().isGenderAdmin()) {
            throw BusinessException.forbidden("Only boys and girls admins can manage meal registration rules");
        }

        return admin.getRole().managedGender();
    }

    private void requireStudent(UserAccount student) {
        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.forbidden("Only students can use this endpoint");
        }
    }

    private Gender requireStudentGender(UserAccount student) {
        if (student.getGender() == null) {
            throw BusinessException.conflict("The student account does not have a gender assigned yet");
        }

        return student.getGender();
    }

    private MealRegistrationRuleView toView(MealRegistrationRule rule) {
        return new MealRegistrationRuleView(
            rule.getMealType(),
            rule.getGenderScope(),
            rule.getRegistrationDate(),
            rule.isActive()
        );
    }

    public record UpsertMealRegistrationRuleCommand(
        MealType mealType,
        LocalDate registrationDate,
        boolean active
    ) {
    }

    public record MealRegistrationRuleView(
        MealType mealType,
        Gender genderScope,
        LocalDate registrationDate,
        boolean active
    ) {
    }
}
