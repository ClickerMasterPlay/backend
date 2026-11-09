package com.resourcemagnate.app.dailyreward.repository;

import com.resourcemagnate.app.dailyreward.model.DailyRewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyRewardClaimRepository
        extends JpaRepository<DailyRewardClaim, UUID> {

    Optional<DailyRewardClaim> findByIdempotencyKey(UUID idempotencyKey);
}
