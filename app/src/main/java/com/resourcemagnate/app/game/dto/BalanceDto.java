package com.resourcemagnate.app.game.dto;

import java.util.Map;

public record BalanceDto(
        long credits,
        Map<String, Long> resources
) {
}
