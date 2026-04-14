package com.sdu.dorm_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bed_unit")
public class BedUnit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomUnit room;

    @Column(name = "bed_number", nullable = false)
    private Integer bedNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupant_id")
    private UserAccount occupant;
}
