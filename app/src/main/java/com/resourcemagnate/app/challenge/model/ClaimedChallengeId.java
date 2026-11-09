package com.resourcemagnate.app.challenge.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClaimedChallengeId implements Serializable {
    private UUID userId;
    private Integer challengeIndex;
}
