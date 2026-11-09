package com.resourcemagnate.app.game.repository;

import com.resourcemagnate.app.game.model.ProcessedClickBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedClickBatchRepository
        extends JpaRepository<ProcessedClickBatch, Long> {

    Optional<ProcessedClickBatch> findByUserIdAndClientBatchId(
            UUID userId,
            UUID clientBatchId
    );

    @Modifying
    @Query("delete from ProcessedClickBatch batch where batch.createdAt < :cutoff")
    int deleteCreatedBefore(Instant cutoff);
}
