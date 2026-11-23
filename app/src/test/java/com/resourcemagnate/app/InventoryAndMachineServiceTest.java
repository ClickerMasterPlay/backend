package com.resourcemagnate.app;

import com.resourcemagnate.app.economy.*;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.dto.MachineStateResponse;
import com.resourcemagnate.app.economy.dto.ToolEquipmentResponse;
import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.InventoryTool;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
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
                userRepository, toolRepository, machineRepository, catalog,
                progressRepository, clock
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
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(toolRepository.findAllByUserIdOrderByAcquiredAtAsc(userId))
                .thenReturn(List.of(tool));
        when(machineRepository.findAllByUserIdOrderByAcquiredAtAsc(userId))
                .thenReturn(List.of(machine));

        InventoryResponse response = inventoryService.getInventory(userId);

        assertEquals(1, response.tools().size());
        assertEquals("iron_pickaxe", response.tools().getFirst().toolCode());
        assertEquals(1.8, response.tools().getFirst().powerMultiplier());
        assertEquals(false, response.tools().getFirst().equipped());
        assertEquals(1, response.machines().size());
        assertEquals("basic_miner", response.machines().getFirst().machineCode());
        assertEquals(36_000L, response.machines().getFirst().creditsPerHour());
        assertEquals(864_000L, response.machines().getFirst().storageCapacity());
        assertEquals(72_000L, response.machines().getFirst().collectableCredits());
    }

    @Test
    void equipTool_shouldPersistOwnedToolAndReturnEffectivePower() {
        progress.setClickPower(1.5);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(toolRepository.existsByUserIdAndToolCode(userId, "iron_pickaxe"))
                .thenReturn(true);
        when(progressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(progress));

        ToolEquipmentResponse response = inventoryService.equipTool(
                userId, "iron_pickaxe"
        );

        assertEquals("iron_pickaxe", progress.getEquippedToolCode());
        assertEquals("iron_pickaxe", response.equippedToolCode());
        assertEquals(1.8, response.toolMultiplier());
        assertEquals(2.7, response.effectiveClickPower(), 1e-9);
        verify(progressRepository).saveAndFlush(progress);
    }

    @Test
    void collect_shouldAddCreditsAndPreservePartialProductionTime() {
        // 2h15m at 10/second = 81,000 credits.
        InventoryMachine machine = machine(NOW.minusSeconds(8_100));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(progressRepository.saveAndFlush(progress)).thenReturn(progress);

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(81_000L, response.collectedCredits());
        assertEquals(81_100L, response.balance().credits());
        assertEquals(NOW, machine.getLastCollectedAt());
        verify(progressRepository).saveAndFlush(progress);
        verify(machineRepository).saveAll(List.of(machine));
    }

    @Test
    void collect_shouldNeverCreditMoreThanMachineBank() {
        InventoryMachine machine = machine(NOW.minusSeconds(72 * 3_600L));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(progressRepository.saveAndFlush(progress)).thenReturn(progress);

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(864_000L, response.collectedCredits());
        assertEquals(864_100L, response.balance().credits());
        assertEquals(NOW, machine.getLastCollectedAt());
        verify(progressRepository).saveAndFlush(progress);
        verify(machineRepository).saveAll(List.of(machine));
    }

    @Test
    void backgroundAccrual_shouldCreditBalanceWithoutEmptyingBank() {
        InventoryMachine machine = machine(NOW.minusSeconds(60));
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        collectionService.accrueInBackground(userId);

        assertEquals(700L, progress.getCredits());
        assertEquals(600L, machine.getBankedCredits());
        assertEquals(NOW, machine.getLastCollectedAt());
        verify(ledgerRepository).save(argThat(entry ->
                entry.getOperationType() == LedgerOperationType.OFFLINE_REWARD
                        && entry.getAmount() == 600L
                        && entry.getBalanceAfter() == 700L
        ));

        when(userRepository.existsById(userId)).thenReturn(true);
        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(600L, response.collectedCredits());
        assertEquals(700L, response.balance().credits());
        assertEquals(0L, machine.getBankedCredits());
        verify(progressRepository, times(1)).saveAndFlush(progress);
        verify(machineRepository, times(2)).saveAll(List.of(machine));
    }

    @Test
    void collect_shouldDiscardProductionTimeWhileBankWasFull() {
        InventoryMachine machine = machine(NOW.minusSeconds(3_600));
        machine.setBankedCredits(864_000L);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(864_000L, response.collectedCredits());
        assertEquals(100L, response.balance().credits());
        assertEquals(0L, machine.getBankedCredits());
        assertEquals(NOW, machine.getLastCollectedAt());

        MachineCollectionResponse immediateRepeat = collectionService.collect(userId);
        assertEquals(0L, immediateRepeat.collectedCredits());
        assertEquals(100L, immediateRepeat.balance().credits());
        verify(progressRepository, never()).saveAndFlush(any());
    }

    @Test
    void collect_shouldNotWriteWhenNothingAccumulated() {
        InventoryMachine machine = machine(NOW.minusMillis(50));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findAllByUserIdForUpdate(userId))
                .thenReturn(List.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        MachineCollectionResponse response = collectionService.collect(userId);

        assertEquals(0L, response.collectedCredits());
        assertEquals(new BalanceResponse(100L, Map.of()), response.balance());
        verify(progressRepository, never()).saveAndFlush(any());
        verify(machineRepository, never()).saveAll(any());
    }

    @Test
    void calculator_shouldProduceTenUnitsPerHour() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.minusSeconds(9_000), NOW, 10, 240
        );

        assertEquals(25L, accrual.amount());
        assertEquals(NOW, accrual.collectedThrough());
    }

    @Test
    void calculator_shouldCapOfflineProductionAtBankCapacity() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.minusSeconds(72 * 3_600L), NOW, 10, 240
        );

        assertEquals(240L, accrual.amount());
        assertEquals(NOW, accrual.collectedThrough());
    }

    @Test
    void calculator_shouldNeverAccrueMoreThanTwentyFourHours() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.minusSeconds(72 * 3_600L), NOW, 10, 1_000
        );

        assertEquals(240L, accrual.amount());
        assertEquals(NOW, accrual.collectedThrough());
    }

    @Test
    void calculator_shouldUseExplicitCapacityInsteadOfClientOnlineTime() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.minusSeconds(72 * 3_600L), NOW, 10, 50
        );

        assertEquals(50L, accrual.amount());
        assertEquals(NOW, accrual.collectedThrough());
    }

    @Test
    void calculator_shouldRejectFutureCollectionTimestampWithoutReward() {
        var accrual = MachineProductionCalculator.calculate(
                NOW.plusSeconds(3_600), NOW, 10, 240
        );

        assertEquals(0L, accrual.amount());
        assertEquals(NOW.plusSeconds(3_600), accrual.collectedThrough());
    }

    @Test
    void install_shouldStartProductionFromCurrentTime() {
        InventoryMachine machine = machine(NOW.minusSeconds(7_200));
        machine.setInstalled(false);
        machine.setInstalledAt(null);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findByUserIdAndMachineCodeForUpdate(
                userId, "basic_miner"
        )).thenReturn(Optional.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(machineRepository.saveAndFlush(machine)).thenReturn(machine);

        MachineStateResponse response = collectionService.install(
                userId, "basic_miner"
        );

        assertEquals(true, response.installed());
        assertEquals(NOW, response.installedAt());
        assertEquals(NOW, response.lastCollectedAt());
        assertEquals(0L, response.collectedCredits());
    }

    @Test
    void uninstall_shouldCollectAccruedCreditsAndStopMachine() {
        InventoryMachine machine = machine(NOW.minusSeconds(7_200));
        when(userRepository.existsById(userId)).thenReturn(true);
        when(machineRepository.findByUserIdAndMachineCodeForUpdate(
                userId, "basic_miner"
        )).thenReturn(Optional.of(machine));
        when(progressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));
        when(progressRepository.saveAndFlush(progress)).thenReturn(progress);
        when(machineRepository.saveAndFlush(machine)).thenReturn(machine);

        MachineStateResponse response = collectionService.uninstall(
                userId, "basic_miner"
        );

        assertEquals(false, response.installed());
        assertEquals(72_000L, response.collectedCredits());
        assertEquals(72_100L, response.balance().credits());
        assertEquals(NOW, response.lastCollectedAt());
    }

    private InventoryMachine machine(Instant lastCollectedAt) {
        InventoryMachine machine = new InventoryMachine();
        machine.setUserId(userId);
        machine.setMachineCode("basic_miner");
        machine.setAcquiredAt(lastCollectedAt);
        machine.setLastCollectedAt(lastCollectedAt);
        machine.setBankedCredits(0L);
        machine.setInstalled(true);
        machine.setInstalledAt(lastCollectedAt);
        return machine;
    }
}
