package com.resourcemagnate.app.economy;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.dto.BalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/economy")
@RequiredArgsConstructor
public class EconomyController {

    private final EconomyService economyService;

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> balance(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) {
            throw new BusinessException("VALIDATION_ERROR", "X-User-Id is required");
        }
        return ResponseEntity.ok(economyService.getBalance(userId));
    }
}
