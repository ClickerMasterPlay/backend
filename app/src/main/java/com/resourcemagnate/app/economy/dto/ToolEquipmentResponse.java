package com.resourcemagnate.app.economy.dto;

public record ToolEquipmentResponse(
        String equippedToolCode,
        double baseClickPower,
        double toolMultiplier,
        double effectiveClickPower
) {
}
