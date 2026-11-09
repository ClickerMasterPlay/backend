package com.resourcemagnate.app.dailyreward.repository;

import com.resourcemagnate.app.dailyreward.model.DailyRewardProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DailyRewardProgressRepository
        extends JpaRepository<DailyRewardProgress, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select progress from DailyRewardProgress progress where progress.userId = :userId")
    Optional<DailyRewardProgress> findByUserIdForUpdate(@Param("userId") UUID userId);
}
