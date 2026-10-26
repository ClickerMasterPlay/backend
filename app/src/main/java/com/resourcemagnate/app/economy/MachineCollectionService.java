package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
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
                continue;
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

    private void requireUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
    }
}
