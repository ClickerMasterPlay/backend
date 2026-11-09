package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.InventoryMachineResponse;
import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.InventoryToolResponse;
import com.resourcemagnate.app.economy.dto.ToolEquipmentResponse;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.economy.repository.InventoryToolRepository;
import com.resourcemagnate.app.user.UserRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserRepository userRepository;
    private final InventoryToolRepository toolRepository;
    private final InventoryMachineRepository machineRepository;
    private final ProductCatalogService catalogService;
    private final PlayerProgressRepository progressRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID userId) {
        requireUser(userId);
        Instant now = clock.instant();
        PlayerProgress progress = getProgress(userId);

        List<InventoryToolResponse> tools = toolRepository
                .findAllByUserIdOrderByAcquiredAtAsc(userId)
                .stream()
                .map(item -> {
                    ProductDefinition product = catalogService.get(
                            ProductType.TOOL, item.getToolCode()
                    );
                    return new InventoryToolResponse(
                            item.getToolCode(),
                            product.toolMultiplier(),
                            item.getToolCode().equals(progress.getEquippedToolCode()),
                            item.getAcquiredAt()
                    );
                })
                .toList();

        List<InventoryMachineResponse> machines = machineRepository
                .findAllByUserIdOrderByAcquiredAtAsc(userId)
                .stream()
                .map(item -> {
                    ProductDefinition product = catalogService.get(
                            ProductType.MACHINE, item.getMachineCode()
                    );
                    long collectable = item.isInstalled()
                            ? MachineProductionCalculator.calculate(
                                    item.getLastCollectedAt(),
                                    now,
                                    product.productionPerHour()
                            ).amount()
                            : 0L;
                    return new InventoryMachineResponse(
                            item.getMachineCode(),
                            product.producedResource(),
                            product.productionPerHour(),
                            collectable,
                            item.isInstalled(),
                            item.getInstalledAt(),
                            item.getAcquiredAt(),
                            item.getLastCollectedAt()
                    );
                })
                .toList();

        return new InventoryResponse(tools, machines);
    }

    @Transactional
    public ToolEquipmentResponse equipTool(UUID userId, String toolCode) {
        requireUser(userId);
        if (toolCode == null || toolCode.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Tool code is required");
        }
        if (!toolRepository.existsByUserIdAndToolCode(userId, toolCode)) {
            throw new BusinessException("TOOL_NOT_OWNED", "Tool is not owned by the player");
        }
        ProductDefinition tool = catalogService.get(ProductType.TOOL, toolCode);
        PlayerProgress progress = getProgressForUpdate(userId);
        progress.setEquippedToolCode(toolCode);
        progressRepository.saveAndFlush(progress);
        return equipment(progress, tool.toolMultiplier());
    }

    @Transactional
    public ToolEquipmentResponse unequipTool(UUID userId) {
        requireUser(userId);
        PlayerProgress progress = getProgressForUpdate(userId);
        progress.setEquippedToolCode(null);
        progressRepository.saveAndFlush(progress);
        return equipment(progress, 1.0);
    }

    private ToolEquipmentResponse equipment(
            PlayerProgress progress,
            double multiplier
    ) {
        double basePower = progress.getClickPower() == null
                ? 1.0
                : progress.getClickPower();
        return new ToolEquipmentResponse(
                progress.getEquippedToolCode(),
                basePower,
                multiplier,
                basePower * multiplier
        );
    }

    private PlayerProgress getProgress(UUID userId) {
        return progressRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
    }

    private PlayerProgress getProgressForUpdate(UUID userId) {
        return progressRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));
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
