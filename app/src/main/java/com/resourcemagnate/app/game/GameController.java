package com.resourcemagnate.app.game;

import com.resourcemagnate.app.game.dto.ClickBatchRequest;
import com.resourcemagnate.app.game.dto.ClickBatchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/click-batch")
    public ResponseEntity<ClickBatchResponse> processClickBatch(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ClickBatchRequest request
    ) {
        return ResponseEntity.ok(
                gameService.processClickBatch(userId, request)
        );
    }
}
