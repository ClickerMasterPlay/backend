package com.resourcemagnate.app.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerProgressRepository extends JpaRepository<PlayerProgress, UUID> {
    Optional<PlayerProgress> findByUserId(UUID userId);
}