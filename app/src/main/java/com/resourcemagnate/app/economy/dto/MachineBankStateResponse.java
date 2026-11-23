package com.resourcemagnate.app.economy.dto;

public record MachineBankStateResponse(
        String machineCode,
        long releasedCredits,
        long filledCredits,
        long capacity,
        boolean full
) {
}
