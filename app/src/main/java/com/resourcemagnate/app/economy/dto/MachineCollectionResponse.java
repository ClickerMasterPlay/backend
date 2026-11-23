package com.resourcemagnate.app.economy.dto;

import java.time.Instant;

public record MachineCollectionResponse(
        long collectedCredits,
        BalanceResponse balance,
        Instant collectedAt
) {
}
