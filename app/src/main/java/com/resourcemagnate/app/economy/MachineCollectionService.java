package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.dto.MachineStateResponse;
import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MachineCollectionService {

    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final InventoryMachineRepository machineRepository;
    private final ProductCatalogService catalogService;
    private final EconomyService economyService;
    private final Clock clock;

    @Transactional
    public MachineCollectionResponse collect(UUID userId) {
        requireUser(userId);

        var machines = machineRepository.findAllByUserIdForUpdate(userId);
        PlayerProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));

        Instant now = clock.instant();
        Map<String, Long> collected = new HashMap<>();

        for (InventoryMachine machine : machines) {
            if (!machine.isInstalled()) {
                continue;
            }
            collectFromMachine(machine, progress, now, collected);
        }

        if (!collected.isEmpty()) {
            // @Version on PlayerProgress prevents lost resource updates.
            progressRepository.saveAndFlush(progress);
            machineRepository.saveAll(machines);
        }

        return new MachineCollectionResponse(
                Map.copyOf(collected),
                economyService.toBalance(progress),
                now
        );
    }

    @Transactional
    public MachineStateResponse install(UUID userId, String machineCode) {
        requireUser(userId);
        InventoryMachine machine = getMachineForUpdate(userId, machineCode);
        if (machine.isInstalled()) {
            throw new BusinessException(
                    "MACHINE_ALREADY_INSTALLED", "Machine is already installed"
            );
        }

        PlayerProgress progress = getProgress(userId);
        Instant now = clock.instant();
        machine.setInstalled(true);
        machine.setInstalledAt(now);
        machine.setLastCollectedAt(now);
        machineRepository.saveAndFlush(machine);

        return machineState(machine, Map.of(), progress);
    }

    @Transactional
    public MachineStateResponse uninstall(UUID userId, String machineCode) {
        requireUser(userId);
        InventoryMachine machine = getMachineForUpdate(userId, machineCode);
        if (!machine.isInstalled()) {
            throw new BusinessException(
                    "MACHINE_NOT_INSTALLED", "Machine is not installed"
            );
        }

        PlayerProgress progress = getProgress(userId);
        Instant now = clock.instant();
        Map<String, Long> collected = new HashMap<>();
        collectFromMachine(machine, progress, now, collected);

        machine.setInstalled(false);
        machine.setLastCollectedAt(now);
        if (!collected.isEmpty()) {
            progressRepository.saveAndFlush(progress);
        }
        machineRepository.saveAndFlush(machine);

        return machineState(machine, Map.copyOf(collected), progress);
    }

    private void collectFromMachine(
            InventoryMachine machine,
            PlayerProgress progress,
            Instant now,
            Map<String, Long> collected
    ) {
        ProductDefinition product = catalogService.get(
                ProductType.MACHINE, machine.getMachineCode()
        );
        MachineProductionCalculator.Accrual accrual =
                MachineProductionCalculator.calculate(
                        machine.getLastCollectedAt(),
                        now,
                        product.productionPerHour()
                );

        if (accrual.amount() == 0) {
            return;
        }

        economyService.addResource(
                progress, product.producedResource(), accrual.amount()
        );
        collected.merge(
                product.producedResource(),
                accrual.amount(),
                Math::addExact
        );
        machine.setLastCollectedAt(accrual.collectedThrough());
    }

    private InventoryMachine getMachineForUpdate(UUID userId, String machineCode) {
        if (machineCode == null || machineCode.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Machine code is required");
        }
        return machineRepository
                .findByUserIdAndMachineCodeForUpdate(userId, machineCode)
                .orElseThrow(() -> new BusinessException(
                        "MACHINE_NOT_OWNED", "Machine is not owned by the player"
                ));
    }

    private PlayerProgress getProgress(UUID userId) {
        return progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
    }

    private MachineStateResponse machineState(
            InventoryMachine machine,
            Map<String, Long> collected,
            PlayerProgress progress
    ) {
        return new MachineStateResponse(
                machine.getMachineCode(),
                machine.isInstalled(),
                machine.getInstalledAt(),
                machine.getLastCollectedAt(),
                collected,
                economyService.toBalance(progress)
        );
    }

    private void requireUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
    }
}
