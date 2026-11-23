package com.resourcemagnate.app.economy.catalog;

import com.resourcemagnate.app.economy.model.ProductType;

public record ProductDefinition(
        ProductType type,
        String id,
        long price,
        Double clickPowerIncrease,
        Double toolMultiplier,
        Long productionPerHour,
        Long storageCapacity,
        String producedResource,
        String effect
) {
}
