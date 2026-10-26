package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.model.LedgerEntry;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.repository.LedgerRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EconomyService {

    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }

        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
        return toBalance(progress);
    }

    public void debitCredits(PlayerProgress progress, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Debit amount must not be negative");
        }
        long current = requireValidCredits(progress);
        if (current < amount) {
            throw new BusinessException(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient credits: required " + amount + ", available " + current
            );
        }
        progress.setCredits(current - amount);
    }

    public void creditCredits(PlayerProgress progress, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Credit amount must not be negative");
        }
        try {
            progress.setCredits(Math.addExact(requireValidCredits(progress), amount));
        } catch (ArithmeticException ex) {
            throw new IllegalStateException("Credits balance overflow", ex);
        }
    }

    public void addResource(PlayerProgress progress, String resourceCode, long amount) {
        if (resourceCode == null || resourceCode.isBlank()) {
            throw new IllegalArgumentException("Resource code is required");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Resource amount must not be negative");
        }

        Map<String, Object> resources = new HashMap<>();
        if (progress.getResources() != null) {
            resources.putAll(progress.getResources());
        }

        long current = readResourceAmount(resources.get(resourceCode));
        try {
            resources.put(resourceCode, Math.addExact(current, amount));
        } catch (ArithmeticException ex) {
            throw new IllegalStateException(
                    "Resource balance overflow: " + resourceCode, ex
            );
        }
        progress.setResources(resources);
    }

    public void increaseClickPower(PlayerProgress progress, double increase) {
        double current = progress.getClickPower() == null ? 1.0 : progress.getClickPower();
        double updated = current + increase;
        if (!Double.isFinite(increase) || increase <= 0 || !Double.isFinite(updated)) {
            throw new IllegalArgumentException("Invalid click power increase");
        }
        progress.setClickPower(updated);
    }

    public LedgerEntry recordCurrencyOperation(
            UUID userId,
            LedgerOperationType operationType,
            long amount,
            ProductType productType,
            String productId,
            UUID purchaseId,
            long balanceAfter
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        entry.setOperationType(operationType);
        entry.setAmount(amount);
        entry.setProductType(productType);
        entry.setProductId(productId);
        entry.setPurchaseId(purchaseId);
        entry.setBalanceAfter(balanceAfter);
        return ledgerRepository.save(entry);
    }

    public BalanceResponse toBalance(PlayerProgress progress) {
        Map<String, Long> resources = new HashMap<>();
        if (progress.getResources() != null) {
            progress.getResources().forEach((code, value) ->
                    resources.put(code, readResourceAmount(value))
            );
        }
        return new BalanceResponse(requireValidCredits(progress), Map.copyOf(resources));
    }

    private long requireValidCredits(PlayerProgress progress) {
        Long credits = progress.getCredits();
        if (credits == null || credits < 0) {
            throw new IllegalStateException("Invalid credits balance");
        }
        return credits;
    }

    private long readResourceAmount(Object value) {
        if (value == null) {
            return 0L;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Resource balance must be numeric: " + value);
        }
        long amount = number.longValue();
        if (number.doubleValue() != amount || amount < 0) {
            throw new IllegalStateException("Invalid resource balance: " + value);
        }
        return amount;
    }
}
