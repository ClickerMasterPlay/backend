package com.resourcemagnate.app.economy.dto;

import java.util.List;

public record InventoryResponse(
        List<InventoryToolResponse> tools,
        List<InventoryMachineResponse> machines
) {
}
