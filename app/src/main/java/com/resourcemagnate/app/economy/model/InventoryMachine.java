package com.resourcemagnate.app.economy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_machines")
@IdClass(InventoryMachineId.class)
@Getter
@Setter
@NoArgsConstructor
public class InventoryMachine {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "machine_code", nullable = false, length = 32)
    private String machineCode;

    @CreationTimestamp
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private Instant acquiredAt;

    @Column(name = "last_collected_at", nullable = false)
    private Instant lastCollectedAt;
}
