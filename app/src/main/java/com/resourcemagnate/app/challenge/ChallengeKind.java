package com.resourcemagnate.app.challenge;

public enum ChallengeKind {
    CLICKS("clicks"),
    BREAK("break"),
    EARN("earn"),
    SPEND("spend"),
    LEVEL("level"),
    BUY("buy"),
    POWER("power");

    private final String apiValue;

    ChallengeKind(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
