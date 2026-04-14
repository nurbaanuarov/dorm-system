package com.sdu.dorm_system.config;

import com.sdu.dorm_system.domain.BedUnit;
import com.sdu.dorm_system.domain.FloorUnit;
import com.sdu.dorm_system.domain.RoomUnit;
import com.sdu.dorm_system.domain.enums.Block;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.repository.BedUnitRepository;
import com.sdu.dorm_system.repository.FloorUnitRepository;
import com.sdu.dorm_system.repository.RoomUnitRepository;
import com.sdu.dorm_system.service.UserManagementService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DormBootstrapInitializer implements ApplicationRunner {

    private final AppProperties appProperties;
    private final UserManagementService userManagementService;
    private final FloorUnitRepository floorUnitRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BedUnitRepository bedUnitRepository;

    @Override
    public void run(ApplicationArguments args) {
        initializeStructure();
        initializeAdmins();
    }

    private void initializeStructure() {
        if (floorUnitRepository.count() == 0) {
            List<FloorUnit> floors = new ArrayList<>();

            for (Block block : Block.values()) {
                for (int floorNumber = 2; floorNumber <= 5; floorNumber++) {
                    FloorUnit floorUnit = new FloorUnit();
                    floorUnit.setBlock(block);
                    floorUnit.setFloorNumber(floorNumber);
                    floorUnit.setActive(true);
                    floors.add(floorUnit);
                }
            }

            floorUnitRepository.saveAll(floors);
        }

        if (roomUnitRepository.count() == 0) {
            List<RoomUnit> rooms = new ArrayList<>();

            for (FloorUnit floorUnit : floorUnitRepository.findAll()) {
                for (int roomNumber = 1; roomNumber <= 80; roomNumber++) {
                    RoomUnit roomUnit = new RoomUnit();
                    roomUnit.setFloor(floorUnit);
                    roomUnit.setRoomNumber(roomNumber);
                    roomUnit.setCapacity(4);
                    rooms.add(roomUnit);
                }
            }

            roomUnitRepository.saveAll(rooms);
        }

        if (bedUnitRepository.count() == 0) {
            List<BedUnit> beds = new ArrayList<>();

            for (RoomUnit roomUnit : roomUnitRepository.findAll()) {
                for (int bedNumber = 1; bedNumber <= roomUnit.getCapacity(); bedNumber++) {
                    BedUnit bedUnit = new BedUnit();
                    bedUnit.setRoom(roomUnit);
                    bedUnit.setBedNumber(bedNumber);
                    beds.add(bedUnit);
                }
            }

            bedUnitRepository.saveAll(beds);
        }
    }

    private void initializeAdmins() {
        userManagementService.ensureBootstrapAdmin(Role.LEAD_ADMIN, appProperties.bootstrap().leadAdmin());
        userManagementService.ensureBootstrapAdmin(Role.BOYS_ADMIN, appProperties.bootstrap().boysAdmin());
        userManagementService.ensureBootstrapAdmin(Role.GIRLS_ADMIN, appProperties.bootstrap().girlsAdmin());
    }
}
