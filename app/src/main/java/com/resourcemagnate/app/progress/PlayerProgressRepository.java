package com.resourcemagnate.app.progress;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerProgressRepository extends JpaRepository<PlayerProgress, UUID> {
    Optional<PlayerProgress> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from PlayerProgress p
        where p.userId = :userId
        """)
    Optional<PlayerProgress> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}