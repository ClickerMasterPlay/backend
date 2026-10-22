package com.resourcemagnate.app.game.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClickBatchRequest(
        @NotNull
        UUID clientBatchId,

        @NotNull
        Integer clicks
) {
}
