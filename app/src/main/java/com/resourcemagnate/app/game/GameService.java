package com.resourcemagnate.app.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.EconomyService;
import com.resourcemagnate.app.economy.model.LedgerOperationType;
import com.resourcemagnate.app.game.dto.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final int MIN_CLICKS = 1;
    private static final int MAX_CLICKS = 50;
    private static final String DEFAULT_BLOCK_CODE = "stone";

    private final UserRepository userRepository;
    private final PlayerProgressRepository progressRepository;
    private final BlockTypeRepository blockTypeRepository;
    private final CurrentBlockRepository currentBlockRepository;
    private final ToolRepository toolRepository;
    private final ProcessedClickBatchRepository batchRepository;
    private final ClickRateLimiter clickRateLimiter;
    private final ObjectMapper objectMapper;
    private final EconomyService economyService;

    @Transactional
    public BlockStateDto getCurrentBlock(UUID userId) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND", "User not found"
                ));

        CurrentBlock block = currentBlockRepository.findByUserId(userId)
                .orElseGet(() -> currentBlockRepository.save(
                        createCurrentBlock(user, getDefaultBlockType())
                ));
        return toBlockDto(block);
    }

    @Transactional
    public ClickBatchResponse processClickBatch(
            UUID userId,
            ClickBatchRequest request
    ) {
        validateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND", "User not found"
                ));

        // Сериализуем игровые операции одного игрока.
        PlayerProgress progress = progressRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(
                        "PROGRESS_NOT_FOUND", "Player progress not found"
                ));

        // Проверяем идемпотентность до rate limit.
        var existingBatch = batchRepository
                .findByUserIdAndClientBatchId(
                        userId, request.clientBatchId()
                );

        if (existingBatch.isPresent()) {
            ProcessedClickBatch saved = existingBatch.get();

            if (!saved.getClicks().equals(request.clicks())) {
                throw new BusinessException(
                        "IDEMPOTENCY_KEY_REUSED",
                        "clientBatchId was already used with different clicks"
                );
            }

            return deserializeResponse(saved.getResponseJson());
        }

        if (!clickRateLimiter.allow(userId)) {
            throw new BusinessException(
                    "RATE_LIMIT_EXCEEDED",
                    "Please wait before sending another click batch"
            );
        }

        CurrentBlock currentBlock = currentBlockRepository
                .findByUserIdForUpdate(userId)
                .orElseGet(() -> createCurrentBlock(user, getDefaultBlockType()));

        double toolMultiplier = getToolMultiplier(
                progress.getEquippedToolCode()
        );

        double clickPower = progress.getClickPower() == null
                ? 1.0
                : progress.getClickPower();

        if (!Double.isFinite(clickPower) || clickPower <= 0) {
            throw new IllegalStateException(
                    "Invalid click_power for user " + userId
            );
        }

        // Урон округляем вверх, чтобы дробный урон не терялся.
        double rawDamage = request.clicks() * clickPower * toolMultiplier;

        if (!Double.isFinite(rawDamage) || rawDamage <= 0
                || rawDamage > Long.MAX_VALUE) {
            throw new IllegalStateException("Calculated damage is invalid");
        }

        long damage = (long) Math.ceil(rawDamage);

        BlockType destroyedBlockType = currentBlock.getBlockType();
        int remainingHp = currentBlock.getCurrentHp();

        if (remainingHp <= 0) {
            throw new IllegalStateException(
                    "Current block has invalid HP: " + remainingHp
            );
        }

        if (damage < remainingHp) {
            currentBlock.setCurrentHp((int) (remainingHp - damage));
            currentBlockRepository.save(currentBlock);

            ClickBatchResponse response = new ClickBatchResponse(
                    false,
                    toBlockDto(currentBlock),
                    new RewardsDto(0L, Map.of()),
                    toBalanceDto(progress)
            );

            saveProcessedBatch(userId, request, response);
            return response;
        }

        // Блок уничтожен: начисляем награды.
        long creditsReward = destroyedBlockType.getRewardCredits() == null
                ? 0L
                : destroyedBlockType.getRewardCredits();

        if (creditsReward < 0) {
            throw new IllegalStateException("Negative block credit reward");
        }

        economyService.creditCredits(progress, creditsReward);

        Map<String, Long> earnedResources = new HashMap<>();

        String resourceCode = destroyedBlockType.getRewardResource();
        int resourceAmount = destroyedBlockType.getRewardAmount() == null
                ? 0
                : destroyedBlockType.getRewardAmount();

        if (resourceAmount < 0) {
            throw new IllegalStateException("Negative block resource reward");
        }

        if (resourceCode != null && !resourceCode.isBlank()
                && resourceAmount > 0) {
            economyService.addResource(progress, resourceCode, resourceAmount);
            earnedResources.put(resourceCode, (long) resourceAmount);
        }

        progressRepository.save(progress);

        economyService.recordCurrencyOperation(
                userId,
                LedgerOperationType.CLICK_REWARD,
                creditsReward,
                null,
                destroyedBlockType.getCode(),
                null,
                progress.getCredits()
        );

        // Удаляем старый блок и сбрасываем изменения в БД,
        // прежде чем создавать новый с тем же user_id.
        currentBlockRepository.delete(currentBlock);
        currentBlockRepository.flush();

        CurrentBlock nextBlock = createCurrentBlock(
                user, getDefaultBlockType()
        );

        currentBlockRepository.save(nextBlock);
        currentBlockRepository.flush();

        ClickBatchResponse response = new ClickBatchResponse(
                true,
                toBlockDto(nextBlock),
                new RewardsDto(creditsReward, Map.copyOf(earnedResources)),
                toBalanceDto(progress)
        );

        saveProcessedBatch(userId, request, response);
        return response;
    }

    private void validateRequest(ClickBatchRequest request) {
        if (request == null || request.clientBatchId() == null) {
            throw new BusinessException(
                    "VALIDATION_ERROR",
                    "clientBatchId is required"
            );
        }

        if (request.clicks() == null
                || request.clicks() < MIN_CLICKS
                || request.clicks() > MAX_CLICKS) {
            throw new BusinessException(
                    "INVALID_BATCH_SIZE",
                    "clicks must be between 1 and 50"
            );
        }
    }

    private double getToolMultiplier(String equippedToolCode) {
        if (equippedToolCode == null || equippedToolCode.isBlank()) {
            return 1.0;
        }

        Tool tool = toolRepository.findById(equippedToolCode)
                .orElseThrow(() -> new BusinessException(
                        "EQUIPPED_TOOL_NOT_FOUND",
                        "Equipped tool does not exist"
                ));

        Double multiplier = tool.getPowerMultiplier();

        if (multiplier == null || !Double.isFinite(multiplier)
                || multiplier <= 0) {
            throw new IllegalStateException(
                    "Invalid power_multiplier for tool " + equippedToolCode
            );
        }

        return multiplier;
    }

    private BlockType getDefaultBlockType() {
        return blockTypeRepository.findByCode(DEFAULT_BLOCK_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Default block type '" + DEFAULT_BLOCK_CODE
                                + "' is not configured"
                ));
    }

    private CurrentBlock createCurrentBlock(User user, BlockType type) {
        CurrentBlock block = new CurrentBlock();
        block.setUser(user);
        block.setBlockType(type);
        block.setMaxHp(type.getMaxHp());
        block.setCurrentHp(type.getMaxHp());
        return block;
    }

    private BlockStateDto toBlockDto(CurrentBlock block) {
        return new BlockStateDto(
                block.getBlockType().getCode(),
                block.getBlockType().getRarity().name(),
                block.getCurrentHp(),
                block.getMaxHp()
        );
    }

    private BalanceDto toBalanceDto(PlayerProgress progress) {
        Map<String, Long> resources = new HashMap<>();

        if (progress.getResources() != null) {
            progress.getResources().forEach((code, amount) ->
                    resources.put(code, readResourceAmount(amount))
            );
        }

        return new BalanceDto(
                progress.getCredits(),
                Map.copyOf(resources)
        );
    }

    private long readResourceAmount(Object value) {
        if (value == null) {
            return 0L;
        }

        if (!(value instanceof Number number)) {
            throw new IllegalStateException(
                    "Resource balance must be numeric, got: " + value
            );
        }

        long amount = number.longValue();

        if (number.doubleValue() != amount || amount < 0) {
            throw new IllegalStateException(
                    "Invalid resource balance: " + value
            );
        }

        return amount;
    }

    private void saveProcessedBatch(
            UUID userId,
            ClickBatchRequest request,
            ClickBatchResponse response
    ) {
        ProcessedClickBatch batch = new ProcessedClickBatch();
        batch.setUserId(userId);
        batch.setClientBatchId(request.clientBatchId());
        batch.setClicks(request.clicks());
        batch.setResponseJson(objectMapper.valueToTree(response));

        batchRepository.save(batch);
    }

    private ClickBatchResponse deserializeResponse(
            com.fasterxml.jackson.databind.JsonNode json
    ) {
        try {
            return objectMapper.treeToValue(json, ClickBatchResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Cannot deserialize stored click batch response", e
            );
        }
    }
}
