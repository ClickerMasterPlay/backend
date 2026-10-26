package com.resourcemagnate.app;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.game.ClickRateLimiter;
import com.resourcemagnate.app.game.GameService;
import com.resourcemagnate.app.game.dto.ClickBatchRequest;
import com.resourcemagnate.app.game.dto.ClickBatchResponse;
import com.resourcemagnate.app.game.model.BlockRarity;
import com.resourcemagnate.app.game.model.BlockType;
import com.resourcemagnate.app.game.model.CurrentBlock;
import com.resourcemagnate.app.game.model.ProcessedClickBatch;
import com.resourcemagnate.app.game.repository.BlockTypeRepository;
import com.resourcemagnate.app.game.repository.CurrentBlockRepository;
import com.resourcemagnate.app.game.repository.ProcessedClickBatchRepository;
import com.resourcemagnate.app.game.repository.ToolRepository;
import com.resourcemagnate.app.progress.PlayerProgress;
import com.resourcemagnate.app.progress.PlayerProgressRepository;
import com.resourcemagnate.app.user.User;
import com.resourcemagnate.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerProgressRepository progressRepository;

    @Mock
    private BlockTypeRepository blockTypeRepository;

    @Mock
    private CurrentBlockRepository currentBlockRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ProcessedClickBatchRepository batchRepository;

    @Mock
    private ClickRateLimiter clickRateLimiter;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GameService gameService;

    private UUID userId;
    private UUID batchId;
    private User user;
    private PlayerProgress progress;
    private BlockType blockType;
    private CurrentBlock currentBlock;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        batchId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        progress = new PlayerProgress();
        progress.setUserId(userId);
        progress.setCredits(100L);
        progress.setClickPower(1.0);
        progress.setEquippedToolCode(null);
        progress.setResources(Map.of());

        blockType = new BlockType();
        blockType.setCode("stone");
        blockType.setRarity(BlockRarity.COMMON);
        blockType.setMaxHp(100);
        blockType.setRewardCredits(5L);
        blockType.setRewardResource(null);
        blockType.setRewardAmount(0);

        currentBlock = new CurrentBlock();
        currentBlock.setUserId(userId);
        currentBlock.setUser(user);
        currentBlock.setBlockType(blockType);
        currentBlock.setCurrentHp(100);
        currentBlock.setMaxHp(100);
    }

    @Test
    void processClickBatch_shouldDamageBlockAndSaveResponse() {
        ClickBatchRequest request = new ClickBatchRequest(batchId, 10);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(progressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(progress));

        when(batchRepository.findByUserIdAndClientBatchId(userId, batchId))
                .thenReturn(Optional.empty());

        when(clickRateLimiter.allow(userId)).thenReturn(true);

        when(currentBlockRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(currentBlock));

        // 10 кликов * 1.0 силы = 10 урона.
        when(objectMapper.valueToTree(any(ClickBatchResponse.class)))
                .thenReturn(new ObjectMapper().createObjectNode());

        ClickBatchResponse response =
                gameService.processClickBatch(userId, request);

        assertFalse(response.destroyed());
        assertEquals(90, response.block().currentHp());
        assertEquals(100L, response.balance().credits());

        verify(currentBlockRepository).save(currentBlock);
        verify(batchRepository).save(any(ProcessedClickBatch.class));

        verify(progressRepository, never()).save(any(PlayerProgress.class));
        verify(currentBlockRepository, never()).delete(any(CurrentBlock.class));
    }

    @Test
    void processClickBatch_shouldRejectWhenRateLimitExceeded() {
        ClickBatchRequest request = new ClickBatchRequest(batchId, 10);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(progressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(progress));

        when(batchRepository.findByUserIdAndClientBatchId(userId, batchId))
                .thenReturn(Optional.empty());

        when(clickRateLimiter.allow(userId)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> gameService.processClickBatch(userId, request)
        );

        assertEquals("RATE_LIMIT_EXCEEDED", exception.getErrorCode());

        assertEquals(100L, progress.getCredits());

        verify(currentBlockRepository, never())
                .findByUserIdForUpdate(any(UUID.class));

        verify(currentBlockRepository, never())
                .save(any(CurrentBlock.class));

        verify(progressRepository, never())
                .save(any(PlayerProgress.class));

        verify(batchRepository, never())
                .save(any(ProcessedClickBatch.class));
    }
}
