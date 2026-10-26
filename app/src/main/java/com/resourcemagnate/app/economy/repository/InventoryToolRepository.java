package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.InventoryTool;
import com.resourcemagnate.app.economy.model.InventoryToolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryToolRepository
        extends JpaRepository<InventoryTool, InventoryToolId> {
    boolean existsByUserIdAndToolCode(UUID userId, String toolCode);
}
