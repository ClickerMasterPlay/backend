package com.resourcemagnate.app.economy.dto;

import java.time.Instant;
import java.util.Map;

public record MachineStateResponse(
        String machineCode,
        boolean installed,
        Instant installedAt,
        Instant lastCollectedAt,
        Map<String, Long> collectedResources,
        BalanceResponse balance
) {
}
