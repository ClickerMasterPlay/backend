package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.MachineBankStateResponse;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.dto.MachineStateResponse;
import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
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
import java.util.ArrayList;
import java.util.List;
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
        long newlyCredited = accrueFromMachines(machines, progress, now);
        BankReset bankReset = resetBanks(machines, now);

        if (newlyCredited > 0) {
            progressRepository.saveAndFlush(progress);
            recordMachineCredits(userId, newlyCredited, progress);
        }
        if (newlyCredited > 0 || bankReset.releasedCredits() > 0) {
            machineRepository.saveAll(machines);
        }

        return new MachineCollectionResponse(
                newlyCredited,
                economyService.toBalance(progress),
                now,
                bankReset.states()
        );
    }

    @Transactional
    public void accrueInBackground(UUID userId) {
        var machines = machineRepository.findAllByUserIdForUpdate(userId);
        if (machines.isEmpty()) {
            return;
        }

        PlayerProgress progress = getProgress(userId);
        long newlyCredited = accrueFromMachines(
                machines, progress, clock.instant()
        );
        if (newlyCredited == 0) {
            return;
        }

        progressRepository.saveAndFlush(progress);
        machineRepository.saveAll(machines);
        recordMachineCredits(userId, newlyCredited, progress);
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
        machine.setBankedCredits(0L);
        machineRepository.saveAndFlush(machine);

        return machineState(machine, 0L, progress);
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
        long newlyCredited = accrueFromMachine(machine, progress, now);
        long collectedCredits = machine.getBankedCredits();

        machine.setInstalled(false);
        machine.setLastCollectedAt(now);
        machine.setBankedCredits(0L);
        if (newlyCredited > 0) {
            progressRepository.saveAndFlush(progress);
            recordMachineCredits(userId, newlyCredited, progress);
        }
        machineRepository.saveAndFlush(machine);

        return machineState(machine, collectedCredits, progress);
    }

    private long accrueFromMachines(
            Iterable<InventoryMachine> machines,
            PlayerProgress progress,
            Instant now
    ) {
        long total = 0L;
        for (InventoryMachine machine : machines) {
            if (machine.isInstalled()) {
                total = Math.addExact(
                        total,
                        accrueFromMachine(machine, progress, now)
                );
            }
        }
        return total;
    }

    private long accrueFromMachine(
            InventoryMachine machine,
            PlayerProgress progress,
            Instant now
    ) {
        ProductDefinition product = catalogService.get(
                ProductType.MACHINE, machine.getMachineCode()
        );
        long bankedCredits = machine.getBankedCredits();
        if (bankedCredits < 0 || bankedCredits > product.storageCapacity()) {
            throw new IllegalStateException("Invalid machine credit bank state");
        }
        long remainingCapacity = product.storageCapacity() - bankedCredits;
        if (remainingCapacity == 0) {
            return 0L;
        }

        MachineProductionCalculator.Accrual accrual =
                MachineProductionCalculator.calculate(
                        machine.getLastCollectedAt(),
                        now,
                        product.creditsPerHour(),
                        remainingCapacity
                );

        if (accrual.amount() == 0) {
            return 0L;
        }

        economyService.creditCredits(progress, accrual.amount());
        machine.setBankedCredits(Math.addExact(bankedCredits, accrual.amount()));
        machine.setLastCollectedAt(accrual.collectedThrough());
        return accrual.amount();
    }

    private BankReset resetBanks(
            Iterable<InventoryMachine> machines,
            Instant now
    ) {
        long total = 0L;
        List<MachineBankStateResponse> states = new ArrayList<>();
        for (InventoryMachine machine : machines) {
            if (!machine.isInstalled()) {
                continue;
            }
            ProductDefinition product = catalogService.get(
                    ProductType.MACHINE, machine.getMachineCode()
            );
            long bankedCredits = machine.getBankedCredits();
            total = Math.addExact(total, bankedCredits);
            if (bankedCredits == product.storageCapacity()) {
                // Time while the bank was full must not be produced later.
                machine.setLastCollectedAt(now);
            }
            machine.setBankedCredits(0L);
            states.add(new MachineBankStateResponse(
                    machine.getMachineCode(),
                    bankedCredits,
                    0L,
                    product.storageCapacity(),
                    false
            ));
        }
        return new BankReset(total, List.copyOf(states));
    }

    private void recordMachineCredits(
            UUID userId,
            long amount,
            PlayerProgress progress
    ) {
        economyService.recordCurrencyOperation(
                userId,
                LedgerOperationType.OFFLINE_REWARD,
                amount,
                null,
                null,
                null,
                progress.getCredits()
        );
    }

    private record BankReset(
            long releasedCredits,
            List<MachineBankStateResponse> states
    ) {
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
            long collectedCredits,
            PlayerProgress progress
    ) {
        return new MachineStateResponse(
                machine.getMachineCode(),
                machine.isInstalled(),
                machine.getInstalledAt(),
                machine.getLastCollectedAt(),
                collectedCredits,
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
