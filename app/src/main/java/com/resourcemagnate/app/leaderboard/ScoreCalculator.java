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

    private static final double CLICK_POWER_WEIGHT = 10.0;
    private static final double TOOL_MULTIPLIER_WEIGHT = 5.0;
    private static final double MACHINE_PRODUCTION_WEIGHT = 1.0;

    public ScoreBreakdown calculate(PlayerProgress progress, String equippedToolCode) {
        long credits = progress.getCredits() != null ? progress.getCredits() : 0L;
        double clickPower = progress.getClickPower() != null ? progress.getClickPower() : 1.0;
        
        long totalResourcesValue = calculateResourcesValue(progress.getResources());
        
        double toolMultiplier = 1.0;
        if (equippedToolCode != null && !equippedToolCode.isBlank()) {
            try {
                ProductDefinition tool = catalogService.get(ProductType.TOOL, equippedToolCode);
                toolMultiplier = tool.toolMultiplier();
            } catch (Exception ignored) {
                toolMultiplier = 1.0;
            }
        }
        
        double machineProductionValue = 0;
        
        long score = credits + totalResourcesValue 
                + (long) (clickPower * CLICK_POWER_WEIGHT)
                + (long) (toolMultiplier * TOOL_MULTIPLIER_WEIGHT)
                + (long) (machineProductionValue * MACHINE_PRODUCTION_WEIGHT);
        
        return new ScoreBreakdown(
                score,
                credits,
                totalResourcesValue,
                clickPower,
                toolMultiplier,
                machineProductionValue
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