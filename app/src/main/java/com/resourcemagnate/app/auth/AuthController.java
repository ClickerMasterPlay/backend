package com.resourcemagnate.app.auth;

import com.resourcemagnate.app.auth.dto.AuthResponse;
import com.resourcemagnate.app.auth.dto.LoginRequest;
import com.resourcemagnate.app.auth.dto.RegisterRequest;
import com.resourcemagnate.app.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> createGuest() {
        AuthResponse response = authService.createGuest();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody RegisterRequest request) {
        
        if (userId == null) {
            throw new BusinessException("USER_ID_REQUIRED", "X-User-Id header is required");
        }

        AuthResponse response = authService.register(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
}