package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.InventoryMachineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface InventoryMachineRepository
        extends JpaRepository<InventoryMachine, InventoryMachineId> {
    boolean existsByUserIdAndMachineCode(UUID userId, String machineCode);

    List<InventoryMachine> findAllByUserIdOrderByAcquiredAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from InventoryMachine m where m.userId = :userId")
    List<InventoryMachine> findAllByUserIdForUpdate(@Param("userId") UUID userId);
}
