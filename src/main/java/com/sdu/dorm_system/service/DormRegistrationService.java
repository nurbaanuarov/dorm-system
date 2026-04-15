package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.DormRegistrationMealOption;
import com.sdu.dorm_system.domain.DormRegistrationSettings;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.DormRegistrationSettingsRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DormRegistrationService {

    private final DormRegistrationSettingsRepository dormRegistrationSettingsRepository;

    @Transactional
    public DormRegistrationSettingsView upsertSettings(UserAccount actor, UpsertDormRegistrationSettingsCommand command) {
        requireLeadAdmin(actor);
        LocalDate today = LocalDate.now();

        if (command.startDate().isAfter(command.endDate())) {
            throw BusinessException.badRequest("Registration start date must be before or equal to end date");
        }

        if (dormRegistrationSettingsRepository.findTopByOrderByUpdatedAtDesc().isPresent()) {
            throw BusinessException.conflict("Dorm registration settings can only be created once and cannot be changed later");
        }

        Map<MealType, MealAvailabilityCommand> commandByMealType = command.mealOptions()
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                MealAvailabilityCommand::mealType,
                Function.identity(),
                (left, right) -> {
                    throw BusinessException.badRequest("Meal type configuration must be unique");
                }
            ));

        DormRegistrationSettings settings = new DormRegistrationSettings();

        settings.setStartDate(command.startDate());
        settings.setEndDate(command.endDate());
        settings.setCreatedBy(actor);

        settings.getMealOptions().clear();
        for (MealType mealType : MealType.values()) {
            MealAvailabilityCommand mealCommand = commandByMealType.getOrDefault(
                mealType,
                new MealAvailabilityCommand(mealType, false, false)
            );

            if (!mealCommand.available() && mealCommand.includedInPrice()) {
                throw BusinessException.badRequest("A meal type cannot be included in price when it is not available");
            }

            DormRegistrationMealOption option = new DormRegistrationMealOption();
            option.setSettings(settings);
            option.setMealType(mealType);
            option.setAvailable(mealCommand.available());
            option.setIncludedInPrice(mealCommand.available() && mealCommand.includedInPrice());
            settings.getMealOptions().add(option);
        }

        DormRegistrationSettings savedSettings = dormRegistrationSettingsRepository.save(settings);
        return toView(savedSettings, today);
    }

    @Transactional(readOnly = true)
    public DormRegistrationSettingsView getSettings() {
        return dormRegistrationSettingsRepository.findTopByOrderByUpdatedAtDesc()
            .map(settings -> toView(settings, LocalDate.now()))
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public ResolvedMealSelection resolveMealSelectionForRoomRegistration(List<MealType> requestedMealTypes) {
        DormRegistrationSettings settings = getRequiredSettings();
        LocalDate today = LocalDate.now();

        if (today.isBefore(settings.getStartDate()) || today.isAfter(settings.getEndDate())) {
            throw BusinessException.conflict("Dormitory registration is not active on " + today);
        }

        Set<MealType> requested = requestedMealTypes == null ? Set.of() : new LinkedHashSet<>(requestedMealTypes);
        Map<MealType, DormRegistrationMealOption> optionsByMealType = settings.getMealOptions()
            .stream()
            .collect(java.util.stream.Collectors.toMap(DormRegistrationMealOption::getMealType, Function.identity()));

        Set<MealType> finalSelected = new LinkedHashSet<>();
        for (MealType mealType : MealType.values()) {
            DormRegistrationMealOption option = optionsByMealType.get(mealType);
            boolean available = option != null && option.isAvailable();
            boolean includedInPrice = option != null && option.isIncludedInPrice();

            if (includedInPrice) {
                finalSelected.add(mealType);
            }

            if (requested.contains(mealType)) {
                if (!available) {
                    throw BusinessException.conflict(mealType.name() + " is not available in the current dorm registration");
                }

                finalSelected.add(mealType);
            }
        }

        return new ResolvedMealSelection(
            finalSelected.stream().sorted(Comparator.comparingInt(MealType::ordinal)).toList(),
            toView(settings, today)
        );
    }

    @Transactional(readOnly = true)
    public void ensureRoomRegistrationOpen() {
        DormRegistrationSettings settings = getRequiredSettings();
        LocalDate today = LocalDate.now();
        if (today.isBefore(settings.getStartDate()) || today.isAfter(settings.getEndDate())) {
            throw BusinessException.conflict("Dormitory registration is not active on " + today);
        }
    }

    private DormRegistrationSettings getRequiredSettings() {
        return dormRegistrationSettingsRepository.findTopByOrderByUpdatedAtDesc()
            .orElseThrow(() -> BusinessException.conflict("Dormitory registration settings have not been configured yet"));
    }

    private DormRegistrationSettingsView toView(DormRegistrationSettings settings, LocalDate today) {
        Map<MealType, DormRegistrationMealOption> optionsByMealType = settings.getMealOptions()
            .stream()
            .collect(java.util.stream.Collectors.toMap(DormRegistrationMealOption::getMealType, Function.identity()));

        return new DormRegistrationSettingsView(
            settings.getStartDate(),
            settings.getEndDate(),
            !today.isBefore(settings.getStartDate()) && !today.isAfter(settings.getEndDate()),
            Arrays.stream(MealType.values())
                .map(mealType -> {
                    DormRegistrationMealOption option = optionsByMealType.get(mealType);
                    return new MealAvailabilityView(
                        mealType,
                        option != null && option.isAvailable(),
                        option != null && option.isIncludedInPrice()
                    );
                })
                .toList()
        );
    }

    private void requireLeadAdmin(UserAccount actor) {
        if (actor.getRole() != Role.LEAD_ADMIN) {
            throw BusinessException.forbidden("Only the lead admin can manage dorm registration settings");
        }
    }

    public record UpsertDormRegistrationSettingsCommand(
        LocalDate startDate,
        LocalDate endDate,
        List<MealAvailabilityCommand> mealOptions
    ) {
    }

    public record MealAvailabilityCommand(
        MealType mealType,
        boolean available,
        boolean includedInPrice
    ) {
    }

    public record MealAvailabilityView(
        MealType mealType,
        boolean available,
        boolean includedInPrice
    ) {
    }

    public record DormRegistrationSettingsView(
        LocalDate startDate,
        LocalDate endDate,
        boolean activeNow,
        List<MealAvailabilityView> mealOptions
    ) {
    }

    public record ResolvedMealSelection(
        List<MealType> finalMealTypesIncludedInPrice,
        DormRegistrationSettingsView settings
    ) {
    }
}
