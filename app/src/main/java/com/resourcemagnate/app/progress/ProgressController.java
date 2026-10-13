package com.resourcemagnate.app.progress;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.progress.dto.ProgressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/me")
    public ResponseEntity<ProgressResponse> getMyProgress(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        if (userId == null) {
            throw new BusinessException("USER_ID_REQUIRED", "X-User-Id header is required");
        }

        ProgressResponse response = progressService.getProgress(userId);
        return ResponseEntity.ok(response);
    }
}