package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.InventoryMachineId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMachineRepository
        extends JpaRepository<InventoryMachine, InventoryMachineId> {
    boolean existsByUserIdAndMachineCode(UUID userId, String machineCode);
}
