package com.resourcemagnate.app.game.model;

import com.resourcemagnate.app.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "current_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentBlock {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "block_type",
            referencedColumnName = "code",
            nullable = false
    )
    private BlockType blockType;

    @Column(name = "current_hp", nullable = false)
    private Integer currentHp;

    @Column(name = "max_hp", nullable = false)
    private Integer maxHp;

    @CreationTimestamp
    @Column(
            name = "issued_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime issuedAt;
}