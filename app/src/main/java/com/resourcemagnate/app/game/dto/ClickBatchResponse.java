package com.resourcemagnate.app.game.dto;

public record ClickBatchResponse(
        boolean destroyed,
        BlockStateDto block,
        RewardsDto rewards,
        BalanceDto balance
) {
}
