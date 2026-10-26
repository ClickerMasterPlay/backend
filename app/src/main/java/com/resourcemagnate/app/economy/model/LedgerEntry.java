package com.resourcemagnate.app.economy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 32)
    private LedgerOperationType operationType;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 32)
    private ProductType productType;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
