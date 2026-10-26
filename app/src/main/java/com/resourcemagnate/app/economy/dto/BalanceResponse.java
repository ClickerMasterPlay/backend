package com.resourcemagnate.app.economy.dto;

import java.util.Map;

public record BalanceResponse(
        long credits,
        Map<String, Long> resources
) {
}
