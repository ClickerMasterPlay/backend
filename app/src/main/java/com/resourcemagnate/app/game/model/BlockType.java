package com.resourcemagnate.app.game.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "block_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockType {

    @Id
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, length = 16)
    private BlockRarity rarity;

    @Column(name = "max_hp", nullable = false)
    private Integer maxHp;

    @Column(name = "reward_credits", nullable = false)
    private Long rewardCredits = 0L;

    @Column(name = "reward_resource", length = 32)
    private String rewardResource;

    @Column(name = "reward_amount", nullable = false)
    private Integer rewardAmount = 0;
}
