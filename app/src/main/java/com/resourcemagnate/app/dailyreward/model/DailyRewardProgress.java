package com.resourcemagnate.app.dailyreward.model;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_reward_progress")
@Getter
@Setter
@NoArgsConstructor
public class DailyRewardProgress {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cycle_day", nullable = false)
    private int cycleDay = 1;

    @Column(name = "last_claimed_date")
    private LocalDate lastClaimedDate;

    @Column(name = "total_claims", nullable = false)
    private long totalClaims;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
