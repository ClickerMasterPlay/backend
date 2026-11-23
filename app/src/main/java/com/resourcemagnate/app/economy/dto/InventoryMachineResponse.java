package com.resourcemagnate.app.economy.dto;

import java.time.Instant;

public record InventoryMachineResponse(
        String machineCode,
        long productionPerHour,
        long storageCapacity,
        long collectableCredits,
        boolean installed,
        Instant installedAt,
        Instant acquiredAt,
        Instant lastCollectedAt
) {
}
