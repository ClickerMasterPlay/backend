package com.resourcemagnate.app.dailyreward.dto;

import java.time.Instant;
import java.util.Map;

public record DailyRewardClaimResponse(
        int cycleDay,
        Map<String, Long> grantedResources,
        Map<String, Long> resources,
        int nextCycleDay,
        Instant nextAvailableAt
) {
}
