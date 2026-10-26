package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    Optional<Purchase> findByIdempotencyKey(UUID idempotencyKey);
}
