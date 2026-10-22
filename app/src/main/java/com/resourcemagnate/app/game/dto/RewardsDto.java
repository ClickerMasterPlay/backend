package com.resourcemagnate.app.game.dto;

import java.util.Map;

public record RewardsDto(
        long credits,
        Map<String, Long> resources
) {
}
