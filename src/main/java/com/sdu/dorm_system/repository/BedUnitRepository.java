package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.BedUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedUnitRepository extends JpaRepository<BedUnit, UUID> {

    @Query("""
        select b
        from BedUnit b
        join fetch b.room r
        join fetch r.floor f
        left join fetch b.occupant
        where r.id = :roomId
        order by b.bedNumber asc
        """)
    List<BedUnit> findAllByRoomId(@Param("roomId") UUID roomId);

    Optional<BedUnit> findFirstByRoomIdAndOccupantIsNullOrderByBedNumberAsc(UUID roomId);

    Optional<BedUnit> findByOccupantId(UUID occupantId);
}
