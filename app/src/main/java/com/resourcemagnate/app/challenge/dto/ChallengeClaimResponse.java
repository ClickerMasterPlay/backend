package com.resourcemagnate.app.challenge.dto;

import com.resourcemagnate.app.economy.dto.BalanceResponse;

public record ChallengeClaimResponse(
        int challengeIndex,
        long reward,
        BalanceResponse balance,
        ChallengeListResponse challenges
) {
}
