package com.resourcemagnate.app.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "challenge_progress")
@Getter
@Setter
@NoArgsConstructor
public class ChallengeProgress {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "total_clicks", nullable = false)
    private long totalClicks;

    @Column(name = "blocks_broken", nullable = false)
    private long blocksBroken;

    @Column(name = "credits_earned", nullable = false)
    private long creditsEarned;

    @Column(name = "credits_spent", nullable = false)
    private long creditsSpent;

    @Column(name = "purchases_count", nullable = false)
    private long purchasesCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
