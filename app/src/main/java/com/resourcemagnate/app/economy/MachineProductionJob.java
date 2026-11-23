package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MachineProductionJob {

    private static final Logger log =
            LoggerFactory.getLogger(MachineProductionJob.class);

    private final InventoryMachineRepository machineRepository;
    private final MachineCollectionService collectionService;

    @Value("${app.machines.accrual-batch-size:100}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.machines.accrual-delay-ms:60000}",
            initialDelayString = "${app.machines.accrual-initial-delay-ms:60000}"
    )
    public void accrueInstalledMachines() {
        if (batchSize <= 0) {
            throw new IllegalStateException("Machine accrual batch size must be positive");
        }

        int page = 0;
        Slice<UUID> users;
        do {
            users = machineRepository.findInstalledUserIds(
                    PageRequest.of(page, batchSize)
            );
            for (UUID userId : users.getContent()) {
                try {
                    collectionService.accrueInBackground(userId);
                } catch (RuntimeException ex) {
                    log.error(
                            "Failed to accrue installed machines for user {}",
                            userId,
                            ex
                    );
                }
            }
            page++;
        } while (users.hasNext());
    }
}
