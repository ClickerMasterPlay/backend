package com.resourcemagnate.app.challenge.dto;

public record ChallengeStatsResponse(
        long clicks,
        long blocksBroken,
        long earned,
        long spent,
        long buys,
        long level,
        double power
) {
}
