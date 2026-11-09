package com.resourcemagnate.app.challenge.dto;

import java.util.List;

public record ChallengeListResponse(
        List<ChallengeItemResponse> items,
        ChallengeStatsResponse stats,
        List<Integer> claimed,
        int total
) {
}
