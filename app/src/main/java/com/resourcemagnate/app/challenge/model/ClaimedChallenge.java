package com.resourcemagnate.app.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claimed_challenges")
@IdClass(ClaimedChallengeId.class)
@Getter
@Setter
@NoArgsConstructor
public class ClaimedChallenge {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "challenge_index", nullable = false)
    private Integer challengeIndex;

    @Column(name = "reward", nullable = false)
    private long reward;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;
}
