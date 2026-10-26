package com.resourcemagnate.app;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.game.ClickRateLimiter;
import com.resourcemagnate.app.game.GameService;
import com.resourcemagnate.app.game.dto.ClickBatchRequest;
import com.resourcemagnate.app.game.dto.ClickBatchResponse;
import com.resourcemagnate.app.game.model.BlockRarity;
import com.resourcemagnate.app.game.model.BlockType;
import com.resourcemagnate.app.game.model.CurrentBlock;
import com.resourcemagnate.app.game.model.ProcessedClickBatch;
import com.resourcemagnate.app.game.model.Tool;
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

    @Mock
    private EconomyService economyService;

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

    @Test
    void processClickBatch_shouldApplyEquippedToolMultiplier() {
        progress.setEquippedToolCode("iron_pickaxe");
        Tool tool = new Tool();
        tool.setCode("iron_pickaxe");
        tool.setPowerMultiplier(1.8);
        ClickBatchRequest request = new ClickBatchRequest(batchId, 10);

        stubAcceptedClick(request);
        when(toolRepository.findById("iron_pickaxe"))
                .thenReturn(Optional.of(tool));

        ClickBatchResponse response = gameService.processClickBatch(userId, request);

        assertFalse(response.destroyed());
        assertEquals(82, response.block().currentHp());
        verify(toolRepository).findById("iron_pickaxe");
    }

    @Test
    void processClickBatch_shouldRewardDestroyedBlockAndWriteLedger() {
        currentBlock.setCurrentHp(10);
        blockType.setRewardCredits(5L);
        blockType.setRewardResource("stone");
        blockType.setRewardAmount(2);
        ClickBatchRequest request = new ClickBatchRequest(batchId, 10);

        stubAcceptedClick(request);
        when(blockTypeRepository.findByCode("stone"))
                .thenReturn(Optional.of(blockType));
        doAnswer(invocation -> {
            PlayerProgress target = invocation.getArgument(0);
            long amount = invocation.getArgument(1);
            target.setCredits(target.getCredits() + amount);
            return null;
        }).when(economyService).creditCredits(any(PlayerProgress.class), anyLong());
        doAnswer(invocation -> {
            PlayerProgress target = invocation.getArgument(0);
            String code = invocation.getArgument(1);
            long amount = invocation.getArgument(2);
            target.setResources(Map.of(code, amount));
            return null;
        }).when(economyService).addResource(
                any(PlayerProgress.class), anyString(), anyLong()
        );

        ClickBatchResponse response = gameService.processClickBatch(userId, request);

        assertTrue(response.destroyed());
        assertEquals(5L, response.rewards().credits());
        assertEquals(2L, response.rewards().resources().get("stone"));
        assertEquals(105L, response.balance().credits());
        assertEquals(2L, response.balance().resources().get("stone"));
        verify(progressRepository).save(progress);
        verify(economyService).recordCurrencyOperation(
                userId,
                LedgerOperationType.CLICK_REWARD,
                5L,
                null,
                "stone",
                null,
                105L
        );
    }

    @Test
    void getCurrentBlock_shouldReturnExistingBlock() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currentBlockRepository.findByUserId(userId))
                .thenReturn(Optional.of(currentBlock));

        var response = gameService.getCurrentBlock(userId);

        assertEquals("stone", response.type());
        assertEquals(100, response.currentHp());
        assertEquals(100, response.maxHp());
        verify(currentBlockRepository, never()).save(any());
    }

    @Test
    void getCurrentBlock_shouldCreateDefaultBlockWhenMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currentBlockRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(blockTypeRepository.findByCode("stone"))
                .thenReturn(Optional.of(blockType));
        when(currentBlockRepository.save(any(CurrentBlock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = gameService.getCurrentBlock(userId);

        assertEquals("stone", response.type());
        assertEquals(100, response.currentHp());
        verify(currentBlockRepository).save(any(CurrentBlock.class));
    }

    private void stubAcceptedClick(ClickBatchRequest request) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(progressRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(progress));
        when(batchRepository.findByUserIdAndClientBatchId(
                userId, request.clientBatchId()
        )).thenReturn(Optional.empty());
        when(clickRateLimiter.allow(userId)).thenReturn(true);
        when(currentBlockRepository.findByUserIdForUpdate(userId))
                .thenReturn(Optional.of(currentBlock));
        when(objectMapper.valueToTree(any(ClickBatchResponse.class)))
                .thenReturn(new ObjectMapper().createObjectNode());
    }
}
