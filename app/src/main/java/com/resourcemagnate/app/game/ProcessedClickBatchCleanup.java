package com.resourcemagnate.app.game;

import com.resourcemagnate.app.game.repository.ProcessedClickBatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProcessedClickBatchCleanup {

    private static final Logger log =
            LoggerFactory.getLogger(ProcessedClickBatchCleanup.class);

    private final ProcessedClickBatchRepository repository;
    private final Clock clock;

    @Value("${app.click-batches.retention:P30D}")
    private Duration retention;

    @Scheduled(cron = "${app.click-batches.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void cleanup() {
        int removed = repository.deleteCreatedBefore(
                clock.instant().minus(retention)
        );
        if (removed > 0) {
            log.info("Removed {} expired processed click batches", removed);
        }
    }
}
