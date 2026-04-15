package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.BedUnit;
import com.sdu.dorm_system.domain.FloorUnit;
import com.sdu.dorm_system.domain.RoomUnit;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Block;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.BedUnitRepository;
import com.sdu.dorm_system.repository.FloorUnitRepository;
import com.sdu.dorm_system.repository.RoomUnitRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final FloorUnitRepository floorUnitRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BedUnitRepository bedUnitRepository;
    private final MealPlanService mealPlanService;
    private final DormRegistrationService dormRegistrationService;

    @Transactional(readOnly = true)
    public List<RoomAvailability> listRoomsForStudent(UserAccount student, Block block, Integer floorNumber) {
        requireStudent(student);
        dormRegistrationService.ensureRoomRegistrationOpen();
        validateBlockForStudent(student, block);

        FloorUnit floorUnit = floorUnitRepository.findByBlockAndFloorNumber(block, floorNumber)
            .orElseThrow(() -> BusinessException.notFound("The requested floor was not found"));

        if (!floorUnit.isActive()) {
            throw BusinessException.conflict("This floor is not active for student placement");
        }

        return roomUnitRepository.findAllByBlockAndFloor(block, floorNumber)
            .stream()
            .map(room -> {
                long freeBeds = bedUnitRepository.findAllByRoomId(room.getId())
                    .stream()
                    .filter(bed -> bed.getOccupant() == null)
                    .count();

                return new RoomAvailability(
                    room.getId(),
                    block,
                    floorNumber,
                    room.getRoomNumber(),
                    room.getCapacity(),
                    freeBeds,
                    freeBeds > 0
                );
            })
            .toList();
    }

    @Transactional
    public RoomAssignment assignRoom(UserAccount student, UUID roomId, List<MealType> selectedMealTypes) {
        requireStudent(student);
        DormRegistrationService.ResolvedMealSelection resolvedMealSelection =
            dormRegistrationService.resolveMealSelectionForRoomRegistration(selectedMealTypes);

        if (bedUnitRepository.findByOccupantId(student.getId()).isPresent()) {
            throw BusinessException.conflict("The student already has an assigned room");
        }

        RoomUnit room = roomUnitRepository.findDetailedById(roomId)
            .orElseThrow(() -> BusinessException.notFound("Room was not found"));

        if (!room.getFloor().isActive()) {
            throw BusinessException.conflict("This floor is not active for student placement");
        }

        validateBlockForStudent(student, room.getFloor().getBlock());

        BedUnit bed = bedUnitRepository.findFirstByRoomIdAndOccupantIsNullOrderByBedNumberAsc(roomId)
            .orElseThrow(() -> BusinessException.conflict("This room is already full"));

        bed.setOccupant(student);
        BedUnit savedBed = bedUnitRepository.save(bed);
        List<MealType> mealTypesIncludedInPrice =
            mealPlanService.replaceStudentMealPlan(student, resolvedMealSelection.finalMealTypesIncludedInPrice());

        return new RoomAssignment(
            room.getId(),
            room.getFloor().getBlock(),
            room.getFloor().getFloorNumber(),
            room.getRoomNumber(),
            savedBed.getBedNumber(),
            mealTypesIncludedInPrice
        );
    }

    @Transactional(readOnly = true)
    public RoomAssignment getAssignment(UserAccount student) {
        requireStudent(student);

        return bedUnitRepository.findByOccupantId(student.getId())
            .map(bed -> new RoomAssignment(
                bed.getRoom().getId(),
                bed.getRoom().getFloor().getBlock(),
                bed.getRoom().getFloor().getFloorNumber(),
                bed.getRoom().getRoomNumber(),
                bed.getBedNumber(),
                mealPlanService.getSelectedMealTypes(student)
            ))
            .orElse(null);
    }

    @Transactional
    public FloorUnit updateFloorActiveStatus(UserAccount actor, UUID floorId, boolean active) {
        if (actor.getRole() != Role.LEAD_ADMIN) {
            throw BusinessException.forbidden("Only the lead admin can change floor availability");
        }

        FloorUnit floorUnit = floorUnitRepository.findById(floorId)
            .orElseThrow(() -> BusinessException.notFound("Floor was not found"));

        floorUnit.setActive(active);
        return floorUnitRepository.save(floorUnit);
    }

    @Transactional(readOnly = true)
    public List<FloorUnit> listFloors() {
        return floorUnitRepository.findAllByOrderByBlockAscFloorNumberAsc();
    }

    private void requireStudent(UserAccount student) {
        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.forbidden("Only students can use this endpoint");
        }
    }

    private void validateBlockForStudent(UserAccount student, Block block) {
        if (student.getGender() == null || !block.supportsGender(student.getGender())) {
            throw BusinessException.badRequest("This block is not available for the student's gender");
        }
    }

    public record RoomAvailability(
        UUID roomId,
        Block block,
        Integer floorNumber,
        Integer roomNumber,
        Integer capacity,
        long freeBeds,
        boolean available
    ) {
    }

    public record RoomAssignment(
        UUID roomId,
        Block block,
        Integer floorNumber,
        Integer roomNumber,
        Integer bedNumber,
        List<MealType> mealTypesIncludedInPrice
    ) {
    }
}
