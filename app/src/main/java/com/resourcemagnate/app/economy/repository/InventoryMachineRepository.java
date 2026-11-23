package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.InventoryMachineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryMachineRepository
        extends JpaRepository<InventoryMachine, InventoryMachineId> {
    boolean existsByUserIdAndMachineCode(UUID userId, String machineCode);

    List<InventoryMachine> findAllByUserIdOrderByAcquiredAtAsc(UUID userId);

    @Query("""
        select distinct m.userId from InventoryMachine m
        where m.installed = true
        order by m.userId
        """)
    Slice<UUID> findInstalledUserIds(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m from InventoryMachine m
        where m.userId = :userId
        order by m.machineCode
        """)
    List<InventoryMachine> findAllByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m from InventoryMachine m
        where m.userId = :userId and m.machineCode = :machineCode
        """)
    Optional<InventoryMachine> findByUserIdAndMachineCodeForUpdate(
            @Param("userId") UUID userId,
            @Param("machineCode") String machineCode
    );
}
