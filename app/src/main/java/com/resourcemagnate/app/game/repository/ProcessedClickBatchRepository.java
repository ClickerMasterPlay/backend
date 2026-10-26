package com.resourcemagnate.app.game.repository;

import com.resourcemagnate.app.game.model.ProcessedClickBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedClickBatchRepository
        extends JpaRepository<ProcessedClickBatch, Long> {

    Optional<ProcessedClickBatch> findByUserIdAndClientBatchId(
            UUID userId,
            UUID clientBatchId
    );
}