package com.resourcemagnate.app.economy.dto;

import java.time.Instant;
import java.util.Map;

public record MachineCollectionResponse(
        Map<String, Long> collectedResources,
        BalanceResponse balance,
        Instant collectedAt
) {
}
