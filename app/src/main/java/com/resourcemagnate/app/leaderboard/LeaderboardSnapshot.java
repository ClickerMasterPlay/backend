package com.resourcemagnate.app.leaderboard;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardSnapshot {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "score", nullable = false)
    private Long score;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "credits", nullable = false)
    private Long credits;

    @Column(name = "total_resources_value", nullable = false)
    private Long totalResourcesValue;

    @Column(name = "click_power", nullable = false)
    private Double clickPower;

    @CreationTimestamp
    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private Instant snapshotAt;
}