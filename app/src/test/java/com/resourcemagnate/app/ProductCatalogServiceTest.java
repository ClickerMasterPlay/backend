package com.resourcemagnate.app;

import com.resourcemagnate.app.economy.MachineProductionCalculator;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.model.ProductType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductCatalogServiceTest {

    private final ProductCatalogService catalog = new ProductCatalogService();

    @Test
    void catalog_shouldExposeAllThreeCreditMachines() {
        assertMachine("basic_miner", 1_500L, 36_000L, 864_000L);
        assertMachine("advanced_miner", 7_500L, 180_000L, 4_320_000L);
        assertMachine("elite_miner", 30_000L, 720_000L, 17_280_000L);

        List<String> machineIds = catalog.getCatalog().items().stream()
                .filter(item -> item.productType() == ProductType.MACHINE)
                .map(item -> item.productId())
                .toList();

        assertEquals(
                List.of("basic_miner", "advanced_miner", "elite_miner"),
                machineIds
        );
    }

    @Test
    void machines_shouldProduceConfiguredCreditsEverySecond() {
        Instant now = Instant.parse("2026-11-23T12:00:00Z");

        assertOneSecondProduction("basic_miner", now, 10L);
        assertOneSecondProduction("advanced_miner", now, 50L);
        assertOneSecondProduction("elite_miner", now, 200L);
    }

    private void assertMachine(
            String id,
            long price,
            long productionPerHour,
            long storageCapacity
    ) {
        ProductDefinition machine = catalog.get(ProductType.MACHINE, id);
        assertEquals(price, machine.price());
        assertEquals(productionPerHour, machine.productionPerHour());
        assertEquals(storageCapacity, machine.storageCapacity());
    }

    private void assertOneSecondProduction(
            String id,
            Instant now,
            long expectedCredits
    ) {
        ProductDefinition machine = catalog.get(ProductType.MACHINE, id);
        var accrual = MachineProductionCalculator.calculate(
                now.minusSeconds(1),
                now,
                machine.productionPerHour(),
                machine.storageCapacity()
        );
        assertEquals(expectedCredits, accrual.amount());
        assertEquals(now, accrual.collectedThrough());
    }
}
