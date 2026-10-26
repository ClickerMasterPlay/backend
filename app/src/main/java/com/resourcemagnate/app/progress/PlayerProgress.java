package com.resourcemagnate.app.progress;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "player_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerProgress {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credits", nullable = false)
    @Builder.Default
    private Long credits = 0L;

    @Column(name = "click_power", nullable = false)
    @Builder.Default
    private Double clickPower = 1.0;

    @Column(name = "equipped_tool_code")
    private String equippedToolCode;

    @Column(name = "resources", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> resources = Map.of();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}