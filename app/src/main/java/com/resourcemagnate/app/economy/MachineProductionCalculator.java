package com.resourcemagnate.app.economy;

import java.time.Instant;
import java.math.BigInteger;

public final class MachineProductionCalculator {

    private static final BigInteger NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger NANOS_PER_HOUR = BigInteger.valueOf(3_600_000_000_000L);

    private MachineProductionCalculator() {
    }

    public static Accrual calculate(
            Instant lastCollectedAt,
            Instant now,
            long productionPerHour,
            long storageCapacity
    ) {
        if (lastCollectedAt == null || now == null) {
            throw new IllegalArgumentException("Collection timestamps are required");
        }
        if (productionPerHour <= 0) {
            throw new IllegalArgumentException("Invalid machine production rate");
        }
        if (storageCapacity <= 0) {
            throw new IllegalArgumentException("Invalid machine storage capacity");
        }
        if (!now.isAfter(lastCollectedAt)) {
            return new Accrual(0, lastCollectedAt);
        }

        BigInteger elapsedNanos = elapsedNanos(lastCollectedAt, now);
        BigInteger produced = elapsedNanos
                .multiply(BigInteger.valueOf(productionPerHour))
                .divide(NANOS_PER_HOUR);
        BigInteger capacity = BigInteger.valueOf(storageCapacity);

        if (produced.compareTo(capacity) >= 0) {
            // Once the bank is full, excess production and its partial remainder
            // are discarded. Collection starts a fresh server-time interval.
            return new Accrual(storageCapacity, now);
        }

        long amount = produced.longValueExact();
        if (amount == 0) {
            return new Accrual(0, lastCollectedAt);
        }

        BigInteger consumedNanos = divideCeiling(
                BigInteger.valueOf(amount).multiply(NANOS_PER_HOUR),
                BigInteger.valueOf(productionPerHour)
        );
        BigInteger[] secondsAndNanos = consumedNanos.divideAndRemainder(NANOS_PER_SECOND);
        Instant collectedThrough = lastCollectedAt
                .plusSeconds(secondsAndNanos[0].longValueExact())
                .plusNanos(secondsAndNanos[1].longValueExact());
        return new Accrual(amount, collectedThrough);
    }

    private static BigInteger elapsedNanos(Instant start, Instant end) {
        long seconds = Math.subtractExact(end.getEpochSecond(), start.getEpochSecond());
        long nanos = (long) end.getNano() - start.getNano();
        return BigInteger.valueOf(seconds)
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(nanos));
    }

    private static BigInteger divideCeiling(BigInteger dividend, BigInteger divisor) {
        return dividend.add(divisor).subtract(BigInteger.ONE).divide(divisor);
    }

    public record Accrual(long amount, Instant collectedThrough) {
    }
}
