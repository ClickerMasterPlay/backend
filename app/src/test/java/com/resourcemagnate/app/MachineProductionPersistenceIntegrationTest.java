package com.resourcemagnate.app;

import com.resourcemagnate.app.auth.AuthService;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.MachineCollectionService;
import com.resourcemagnate.app.economy.catalog.ProductCatalogService;
import com.resourcemagnate.app.economy.dto.MachineCollectionResponse;
import com.resourcemagnate.app.economy.model.InventoryMachine;
import com.resourcemagnate.app.economy.repository.InventoryMachineRepository;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.machines.accrual-delay-ms=3600000",
        "app.machines.accrual-initial-delay-ms=3600000"
})
@Import(MachineProductionPersistenceIntegrationTest.ClockTestConfig.class)
class MachineProductionPersistenceIntegrationTest {

    private static final Instant START =
            Instant.parse("2026-11-23T12:00:00Z");

    @Autowired private AuthService authService;
    @Autowired private MachineCollectionService collectionService;
    @Autowired private InventoryMachineRepository machineRepository;
    @Autowired private PlayerProgressRepository progressRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductCatalogService catalogService;
    @Autowired private EconomyService economyService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MutableClock clock;

    private UUID userId;

    @BeforeEach
    void setUpMachine() {
        clock.setInstant(START);
        userId = authService.createGuest().getUserId();

        InventoryMachine machine = new InventoryMachine();
        machine.setUserId(userId);
        machine.setMachineCode("basic_miner");
        machine.setLastCollectedAt(START);
        machine.setBankedCredits(0L);
        machine.setInstalled(true);
        machine.setInstalledAt(START);
        machineRepository.saveAndFlush(machine);
    }

    @AfterEach
    void cleanUpUser() {
        if (userId != null && userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void persistedCursorAndBank_shouldRestoreProductionAfterServiceRestart() {
        clock.setInstant(START.plusSeconds(1));
        collectionService.accrueInBackground(userId);

        var persistedAfterFirstRun = machineRepository
                .findById(machineId()).orElseThrow();
        assertEquals(10L, persistedAfterFirstRun.getBankedCredits());
        assertEquals(START.plusSeconds(1), persistedAfterFirstRun.getLastCollectedAt());
        assertEquals(10L, progressRepository.findById(userId)
                .orElseThrow().getCredits());

        // A new service instance has no in-memory state from the first run and
        // must restore the cursor, bank and balance from the database.
        clock.setInstant(START.plusSeconds(2));
        MachineCollectionService restartedService = new MachineCollectionService(
                userRepository,
                progressRepository,
                machineRepository,
                catalogService,
                economyService,
                clock
        );
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> restartedService.accrueInBackground(userId)
        );

        var restored = machineRepository.findById(machineId()).orElseThrow();
        assertEquals(20L, restored.getBankedCredits());
        assertEquals(START.plusSeconds(2), restored.getLastCollectedAt());
        assertEquals(20L, progressRepository.findById(userId)
                .orElseThrow().getCredits());
    }

    @Test
    void backgroundAndManualCollection_shouldNotDoubleCreditWhenConcurrent()
            throws Exception {
        clock.setInstant(START.plusSeconds(1));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> background = executor.submit(() -> {
                awaitStart(ready, start);
                collectionService.accrueInBackground(userId);
            });
            Future<MachineCollectionResponse> manual = executor.submit(() -> {
                awaitStart(ready, start);
                return collectionService.collect(userId);
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            background.get(10, TimeUnit.SECONDS);
            manual.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        var progress = progressRepository.findById(userId).orElseThrow();
        var machine = machineRepository.findById(machineId()).orElseThrow();
        assertEquals(10L, progress.getCredits());
        assertEquals(0L, machine.getBankedCredits());
        assertEquals(START.plusSeconds(1), machine.getLastCollectedAt());
    }

    private com.resourcemagnate.app.economy.model.InventoryMachineId machineId() {
        return new com.resourcemagnate.app.economy.model.InventoryMachineId(
                userId, "basic_miner"
        );
    }

    private static void awaitStart(
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test start timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted", ex);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockTestConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START);
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        MutableClock(Instant initialInstant) {
            instant = new AtomicReference<>(initialInstant);
        }

        void setInstant(Instant value) {
            instant.set(value);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("Test clock supports UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
