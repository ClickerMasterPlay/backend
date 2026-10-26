package com.resourcemagnate.app.game.dto;

public record BlockStateDto(
        String type,
        String rarity,
        int currentHp,
        int maxHp
) {
}