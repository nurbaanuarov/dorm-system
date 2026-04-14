package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.RoomUnit;
import com.sdu.dorm_system.domain.enums.Block;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomUnitRepository extends JpaRepository<RoomUnit, UUID> {

    @Query("""
        select r
        from RoomUnit r
        join fetch r.floor f
        where f.block = :block and f.floorNumber = :floorNumber
        order by r.roomNumber asc
        """)
    List<RoomUnit> findAllByBlockAndFloor(@Param("block") Block block, @Param("floorNumber") Integer floorNumber);

    @Query("""
        select r
        from RoomUnit r
        join fetch r.floor f
        where r.id = :roomId
        """)
    Optional<RoomUnit> findDetailedById(@Param("roomId") UUID roomId);
}
