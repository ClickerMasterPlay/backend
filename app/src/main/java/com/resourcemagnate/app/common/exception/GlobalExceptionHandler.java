package com.resourcemagnate.app.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Ошибка валидации входных данных";
        }

        return ResponseEntity
                .badRequest()
                .body(ApiError.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business error at {}: {}",
                request.getRequestURI(), ex.getMessage());

        HttpStatus status = switch (ex.getErrorCode()) {
            case "USER_NOT_FOUND", "PROGRESS_NOT_FOUND", "PRODUCT_NOT_FOUND" ->
                    HttpStatus.NOT_FOUND;
            case "RATE_LIMIT_EXCEEDED" ->
                    HttpStatus.TOO_MANY_REQUESTS;
            case "INVALID_BATCH_SIZE" ->
                    HttpStatus.BAD_REQUEST;
            case "INSUFFICIENT_FUNDS", "PRODUCT_ALREADY_OWNED",
                    "IDEMPOTENCY_KEY_REUSED", "DUPLICATE_PURCHASE" ->
                    HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity
                .status(status)
                .body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("Concurrent modification at {}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        "CONCURRENT_MODIFICATION",
                        "Player balance was changed concurrently; retry the request"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error at {}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        "INTERNAL_SERVER_ERROR",
                        "Внутренняя ошибка сервера"
                ));
    }
}
