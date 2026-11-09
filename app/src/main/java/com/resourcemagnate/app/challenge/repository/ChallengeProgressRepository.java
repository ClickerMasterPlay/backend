package com.resourcemagnate.app.challenge.repository;

import com.resourcemagnate.app.challenge.model.ChallengeProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChallengeProgressRepository
        extends JpaRepository<ChallengeProgress, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ChallengeProgress p where p.userId = :userId")
    Optional<ChallengeProgress> findByUserIdForUpdate(@Param("userId") UUID userId);
}
