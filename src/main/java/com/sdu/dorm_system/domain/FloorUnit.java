package com.sdu.dorm_system.domain;

import com.sdu.dorm_system.domain.enums.Block;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "floor_unit")
public class FloorUnit extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "block_code", nullable = false)
    private Block block;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(nullable = false)
    private boolean active = true;
}
