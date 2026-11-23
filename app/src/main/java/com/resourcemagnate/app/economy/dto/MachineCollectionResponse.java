package com.resourcemagnate.app.economy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

public record MachineCollectionResponse(
        long creditedCredits,
        BalanceResponse balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant serverTime,
        List<MachineBankStateResponse> banks
) {
}
