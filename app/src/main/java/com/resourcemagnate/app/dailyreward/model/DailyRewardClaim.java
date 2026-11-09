package com.resourcemagnate.app.dailyreward.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_reward_claims")
@Getter
@Setter
@NoArgsConstructor
public class DailyRewardClaim {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reward_date", nullable = false)
    private LocalDate rewardDate;

    @Column(name = "cycle_day", nullable = false)
    private int cycleDay;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode rewardJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode responseJson;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;
}
