package com.resourcemagnate.app.economy.repository;

import com.resourcemagnate.app.economy.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    Optional<Purchase> findByIdempotencyKey(UUID idempotencyKey);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO purchases (
                id, user_id, product_type, product_id, price, status,
                idempotency_key, response_json
            ) VALUES (
                :id, :userId, :productType, :productId, :price, 'PROCESSING',
                :idempotencyKey, '{}'::jsonb
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int reserve(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("productType") String productType,
            @Param("productId") String productId,
            @Param("price") long price,
            @Param("idempotencyKey") UUID idempotencyKey
    );
}
