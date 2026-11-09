package com.resourcemagnate.app.challenge.dto;

public record ChallengeItemResponse(
        int index,
        String kind,
        double goal,
        long reward,
        double current,
        boolean ready
) {
}
