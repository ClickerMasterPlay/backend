package com.resourcemagnate.app.leaderboard;

import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.progress.PlayerProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    private final ProductCatalogService catalogService;

    public ScoreBreakdown calculate(PlayerProgress progress, String equippedToolCode) {
        long credits = progress.getCredits() != null ? progress.getCredits() : 0L;

        return new ScoreBreakdown(
                credits,
                credits,
                0L,
                0.0,
                0.0,
                0.0
        );
    }
    
    private long calculateResourcesValue(Map<String, Object> resources) {
        if (resources == null || resources.isEmpty()) {
            return 0L;
        }
        
        long total = 0L;
        for (Map.Entry<String, Object> entry : resources.entrySet()) {
            String resourceCode = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Number number) {
                long amount = number.longValue();
                long resourceValue = getResourceBaseValue(resourceCode);
                total += amount * resourceValue;
            }
        }
        return total;
    }
    
    private long getResourceBaseValue(String resourceCode) {
        return switch (resourceCode) {
            case "stone" -> 1;
            case "iron" -> 5;
            case "gold" -> 20;
            case "diamond" -> 100;
            case "netherite" -> 500;
            default -> 1;
        };
    }
    
    public record ScoreBreakdown(
            long score,
            long credits,
            long totalResourcesValue,
            double clickPower,
            double toolMultiplier,
            double machineProductionValue
    ) {}
}