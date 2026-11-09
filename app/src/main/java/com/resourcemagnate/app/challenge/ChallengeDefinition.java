package com.resourcemagnate.app.challenge;

public record ChallengeDefinition(
        int index,
        ChallengeKind kind,
        double goal,
        long reward
) {
}
