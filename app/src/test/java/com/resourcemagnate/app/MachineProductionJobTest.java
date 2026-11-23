package com.resourcemagnate.app;

import com.resourcemagnate.app.economy.MachineCollectionService;
import com.resourcemagnate.app.economy.MachineProductionJob;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineProductionJobTest {

    @Mock private InventoryMachineRepository machineRepository;
    @Mock private MachineCollectionService collectionService;

    @Test
    void job_shouldProcessInstalledMachineOwnersInBatches() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        var firstPage = PageRequest.of(0, 2);
        var secondPage = PageRequest.of(1, 2);
        when(machineRepository.findInstalledUserIds(firstPage)).thenReturn(
                new SliceImpl<>(List.of(first, second), firstPage, true)
        );
        when(machineRepository.findInstalledUserIds(secondPage)).thenReturn(
                new SliceImpl<>(List.of(third), secondPage, false)
        );

        MachineProductionJob job = new MachineProductionJob(
                machineRepository, collectionService
        );
        ReflectionTestUtils.setField(job, "batchSize", 2);

        job.accrueInstalledMachines();

        verify(collectionService).accrueInBackground(first);
        verify(collectionService).accrueInBackground(second);
        verify(collectionService).accrueInBackground(third);
    }
}
