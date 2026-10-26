package com.resourcemagnate.app;

import com.resourcemagnate.app.economy.*;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.InventoryTool;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.economy.repository.InventoryToolRepository;
import com.resourcemagnate.app.economy.repository.LedgerRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAndMachineServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private PlayerProgressRepository progressRepository;
    @Mock private InventoryToolRepository toolRepository;
    @Mock private InventoryMachineRepository machineRepository;
    @Mock private LedgerRepository ledgerRepository;

    private InventoryService inventoryService;
    private MachineCollectionService collectionService;
    private UUID userId;
    private PlayerProgress progress;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        ProductCatalogService catalog = new ProductCatalogService();
        EconomyService economyService = new EconomyService(
                userRepository, progressRepository, ledgerRepository
        );
        inventoryService = new InventoryService(
                userRepository, toolRepository, machineRepository, catalog, clock
        );
        collectionService = new MachineCollectionService(
                userRepository,
                progressRepository,
                machineRepository,
                catalog,
                economyService,
                clock
        );

        userId = UUID.randomUUID();
        progress = new PlayerProgress();
        progress.setUserId(userId);
        progress.setCredits(100L);
        progress.setClickPower(1.0);
        progress.setResources(Map.of());
    }

    @Test
    void inventory_shouldReturnOwnedItemsAndCollectableProduction() {
        InventoryTool tool = new InventoryTool();
        tool.setUserId(userId);
        tool.setToolCode("iron_pickaxe");
        tool.setAcquiredAt(NOW.minusSeconds(10_000));

        InventoryMachine machine = machine(NOW.minusSeconds(7_200));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(toolRepository.findAllByUserIdOrderByAcquiredAtAsc(userId))
                .thenReturn(List.of(tool));
        when(machineRepository.findAllByUserIdOrderByAcquiredAtAsc(userId))
                .thenReturn(List.of(machine));

        InventoryResponse response = inventoryService.getInventory(userId);

        assertEquals(1, response.tools().size());
        assertEquals("iron_pickaxe", response.tools().getFirst().toolCode());
        assertEquals(1.8, response.tools().getFirst().powerMultiplier());
        assertEquals(1, response.machines().size());
        assertEquals("basic_miner", response.machines().getFirst().machineCode());
        assertEquals("stone", response.machines().getFirst().resourceCode());
        assertEquals(10L, response.machines().getFirst().productionPerHour());
        assertEquals(20L, response.machines().getFirst().collectableAmount());
    }

    @Test
    void collect_shouldAddResourcesAndPreservePartialProductionTime() {
        // 2h15m at 10/hour = 22 units, with 3 minutes kept for the next collection.
        InventoryMachine machine = machine(NOW.minusSeconds(8_100));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(progressRepository.saveAndFlush(progress)).thenReturn(progress);

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(22L, response.collectedResources().get("stone"));
        assertEquals(22L, response.balance().resources().get("stone"));
        assertEquals(NOW.minusSeconds(180), machine.getLastCollectedAt());
        verify(progressRepository).saveAndFlush(progress);
        verify(machineRepository).saveAll(List.of(machine));
    }

    @Test
    void collect_shouldNotWriteWhenNothingAccumulated() {
        InventoryMachine machine = machine(NOW.minusSeconds(100));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(Map.of(), response.collectedResources());
        assertEquals(new BalanceResponse(100L, Map.of()), response.balance());
        verify(progressRepository, never()).saveAndFlush(any());
        verify(machineRepository, never()).saveAll(any());
    }

    @Test
    void calculator_shouldProduceTenUnitsPerHour() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.minusSeconds(9_000), NOW, 10
        );

        assertEquals(25L, accrual.amount());
        assertEquals(NOW, accrual.collectedThrough());
    }

    private InventoryMachine machine(Instant lastCollectedAt) {
        InventoryMachine machine = new InventoryMachine();
        machine.setUserId(userId);
        machine.setMachineCode("basic_miner");
        machine.setAcquiredAt(lastCollectedAt);
        machine.setLastCollectedAt(lastCollectedAt);
        return machine;
    }
}
