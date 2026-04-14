package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.FloorUnit;
import com.sdu.dorm_system.domain.enums.Block;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorUnitRepository extends JpaRepository<FloorUnit, UUID> {

    Optional<FloorUnit> findByBlockAndFloorNumber(Block block, Integer floorNumber);

    List<FloorUnit> findAllByOrderByBlockAscFloorNumberAsc();
}
