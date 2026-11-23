package com.resourcemagnate.app.economy.dto;

import java.time.Instant;

public record MachineStateResponse(
        String machineCode,
        boolean installed,
        Instant installedAt,
        Instant lastCollectedAt,
        long collectedCredits,
        BalanceResponse balance
) {
}
