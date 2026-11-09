package com.resourcemagnate.app.economy.dto;

import java.time.Instant;

public record InventoryToolResponse(
        String toolCode,
        double powerMultiplier,
        boolean equipped,
        Instant acquiredAt
) {
}
