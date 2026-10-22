package com.resourcemagnate.app.game.model;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processed_click_batches",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_click_batch_user_id",
                columnNames = {"user_id", "client_batch_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ProcessedClickBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_batch_id", nullable = false)
    private UUID clientBatchId;

    @Column(name = "clicks", nullable = false)
    private Integer clicks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode responseJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
