package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.MealBooking;
import com.sdu.dorm_system.domain.MealSlot;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.MealBookingRepository;
import com.sdu.dorm_system.repository.MealSlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealSlotRepository mealSlotRepository;
    private final MealBookingRepository mealBookingRepository;
    private final MealPlanService mealPlanService;

    @Transactional
    public List<MealSlotView> createSlots(UserAccount admin, CreateSlotsCommand command) {
        Gender scope = resolveManagedGender(admin);

        if (command.slotCount() <= 0 || command.durationMinutes() <= 0 || command.capacity() <= 0) {
            throw BusinessException.badRequest("Slot count, duration, and capacity must be positive");
        }

        List<MealSlot> slots = new ArrayList<>();
        LocalTime currentStart = command.startTime();

        for (int index = 0; index < command.slotCount(); index++) {
            MealSlot slot = new MealSlot();
            slot.setMealType(command.mealType());
            slot.setGenderScope(scope);
            slot.setSlotDate(command.slotDate());
            slot.setStartTime(currentStart);
            slot.setEndTime(currentStart.plusMinutes(command.durationMinutes()));
            slot.setCapacity(command.capacity());
            slot.setActive(true);
            slot.setCreatedBy(admin);
            slots.add(slot);

            currentStart = currentStart.plusMinutes(command.durationMinutes());
        }

        return mealSlotRepository.saveAll(slots)
            .stream()
            .map(slot -> toView(slot, 0))
            .toList();
    }

    @Transactional
    public MealSlotView updateSlot(UserAccount admin, UUID slotId, UpdateSlotCommand command) {
        Gender scope = resolveManagedGender(admin);

        MealSlot slot = mealSlotRepository.findById(slotId)
            .orElseThrow(() -> BusinessException.notFound("Meal slot was not found"));

        if (slot.getGenderScope() != scope) {
            throw BusinessException.forbidden("This admin cannot manage the selected slot");
        }

        LocalTime newStart = command.startTime() != null ? command.startTime() : slot.getStartTime();
        LocalTime newEnd = command.endTime() != null ? command.endTime() : slot.getEndTime();
        if (!newEnd.isAfter(newStart)) {
            throw BusinessException.badRequest("The slot end time must be after the start time");
        }

        if (command.capacity() != null && command.capacity() <= 0) {
            throw BusinessException.badRequest("Capacity must be positive");
        }

        slot.setStartTime(newStart);
        slot.setEndTime(newEnd);

        if (command.capacity() != null) {
            slot.setCapacity(command.capacity());
        }

        if (command.active() != null) {
            slot.setActive(command.active());
        }

        MealSlot savedSlot = mealSlotRepository.save(slot);
        long bookedCount = mealBookingRepository.countBySlotId(savedSlot.getId());
        return toView(savedSlot, bookedCount);
    }

    @Transactional(readOnly = true)
    public Page<MealSlotView> listSlotsForAdmin(UserAccount admin, MealType mealType, LocalDate slotDate, Pageable pageable) {
        Gender scope = resolveManagedGender(admin);
        List<MealSlotView> slots = mealSlotRepository.findAllByGenderScopeAndMealTypeAndSlotDateOrderByStartTimeAsc(scope, mealType, slotDate)
            .stream()
            .map(slot -> toView(slot, mealBookingRepository.countBySlotId(slot.getId())))
            .toList();
        return PaginationUtils.pageList(slots, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MealSlotView> listSlotsForStudent(UserAccount student, MealType mealType, LocalDate slotDate, Pageable pageable) {
        requireStudent(student);

        if (!mealPlanService.isMealTypeIncluded(student, mealType)) {
            return Page.empty(pageable);
        }

        List<MealSlotView> slots = mealSlotRepository.findAllByGenderScopeAndMealTypeAndSlotDateOrderByStartTimeAsc(student.getGender(), mealType, slotDate)
            .stream()
            .map(slot -> toView(slot, mealBookingRepository.countBySlotId(slot.getId())))
            .toList();
        return PaginationUtils.pageList(slots, pageable);
    }

    @Transactional
    public MealSlotView bookSlot(UserAccount student, UUID slotId) {
        requireStudent(student);

        MealSlot slot = mealSlotRepository.findById(slotId)
            .orElseThrow(() -> BusinessException.notFound("Meal slot was not found"));

        if (slot.getGenderScope() != student.getGender()) {
            throw BusinessException.forbidden("This student cannot book the selected slot");
        }

        mealPlanService.ensureMealTypeIncluded(student, slot.getMealType());

        if (mealBookingRepository.existsByStudentIdAndSlotDateAndMealType(student.getId(), slot.getSlotDate(), slot.getMealType())) {
            throw BusinessException.conflict("The student already has a booking for this meal type on the selected date");
        }

        long bookedCount = mealBookingRepository.countBySlotId(slot.getId());
        if (!isAvailable(slot, bookedCount)) {
            throw BusinessException.conflict("This meal slot is no longer available");
        }

        MealBooking booking = new MealBooking();
        booking.setSlot(slot);
        booking.setStudent(student);
        booking.setMealType(slot.getMealType());
        booking.setSlotDate(slot.getSlotDate());
        mealBookingRepository.save(booking);

        return toView(slot, bookedCount + 1);
    }

    private Gender resolveManagedGender(UserAccount admin) {
        if (!admin.getRole().isGenderAdmin()) {
            throw BusinessException.forbidden("Only boys and girls admins can manage meal slots");
        }

        return admin.getRole().managedGender();
    }

    private void requireStudent(UserAccount student) {
        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.forbidden("Only students can use this endpoint");
        }
    }

    private boolean isAvailable(MealSlot slot, long bookedCount) {
        if (!slot.isActive() || bookedCount >= slot.getCapacity()) {
            return false;
        }

        LocalDateTime bookingCutoff = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime()).minusMinutes(20);
        return !LocalDateTime.now().isAfter(bookingCutoff);
    }

    private MealSlotView toView(MealSlot slot, long bookedCount) {
        return new MealSlotView(
            slot.getId(),
            slot.getMealType(),
            slot.getSlotDate(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getCapacity() - bookedCount,
            isAvailable(slot, bookedCount),
            slot.isActive()
        );
    }

    public record CreateSlotsCommand(
        LocalDate slotDate,
        MealType mealType,
        LocalTime startTime,
        int slotCount,
        int durationMinutes,
        int capacity
    ) {
    }

    public record UpdateSlotCommand(
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        Boolean active
    ) {
    }

    public record MealSlotView(
        UUID id,
        MealType mealType,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        long freePlaces,
        boolean available,
        boolean active
    ) {
    }
}
