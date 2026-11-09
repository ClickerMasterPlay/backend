package com.resourcemagnate.app.dailyreward.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record DailyRewardStatusResponse(
        boolean available,
        int cycleDay,
        Map<String, Long> reward,
        LocalDate serverDate,
        Instant nextResetAt
) {
}
