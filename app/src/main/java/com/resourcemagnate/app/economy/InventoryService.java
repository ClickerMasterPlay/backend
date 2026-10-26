package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.catalog.ProductDefinition;
import com.resourcemagnate.app.economy.dto.InventoryMachineResponse;
import com.resourcemagnate.app.economy.dto.InventoryResponse;
import com.resourcemagnate.app.economy.dto.InventoryToolResponse;
import com.resourcemagnate.app.economy.model.ProductType;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.economy.repository.InventoryToolRepository;
import com.resourcemagnate.app.user.UserRepository;
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
    private final Clock clock;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID userId) {
        requireUser(userId);
        Instant now = clock.instant();

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
                    long collectable = MachineProductionCalculator.calculate(
                            item.getLastCollectedAt(), now, product.productionPerHour()
                    ).amount();
                    return new InventoryMachineResponse(
                            item.getMachineCode(),
                            product.producedResource(),
                            product.productionPerHour(),
                            collectable,
                            item.getAcquiredAt(),
                            item.getLastCollectedAt()
                    );
                })
                .toList();

        return new InventoryResponse(tools, machines);
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
