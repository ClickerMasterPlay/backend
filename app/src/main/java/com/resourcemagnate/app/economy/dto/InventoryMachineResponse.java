package com.resourcemagnate.app.economy.dto;

import java.time.Instant;

public record InventoryMachineResponse(
        String machineCode,
        String resourceCode,
        long productionPerHour,
        long collectableAmount,
        Instant acquiredAt,
        Instant lastCollectedAt
) {
}
