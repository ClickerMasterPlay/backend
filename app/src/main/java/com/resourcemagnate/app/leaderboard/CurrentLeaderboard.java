package com.resourcemagnate.app.leaderboard;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "current_leaderboard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentLeaderboard {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "score", nullable = false)
    private Long score;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "credits", nullable = false)
    private Long credits;

    @Column(name = "total_resources_value", nullable = false)
    private Long totalResourcesValue;

    @Column(name = "click_power", nullable = false)
    private Double clickPower;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}