package com.sdxpub.feishubot.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BotException.class)
    public ResponseEntity<Map<String, String>> handleBotException(BotException e) {
        log.error("Bot exception occurred: {}", e.getMessage(), e);
        Map<String, String> response = new HashMap<>();
        response.put("code", e.getCode());
        response.put("message", e.getMessage());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        Map<String, String> response = new HashMap<>();
        response.put("code", ErrorCode.SYSTEM_ERROR.getCode());
        response.put("message", "系统内部错误");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage(), e);
        Map<String, String> response = new HashMap<>();
        response.put("code", ErrorCode.PARAM_INVALID.getCode());
        response.put("message", e.getMessage());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        log.error("Invalid state: {}", e.getMessage(), e);
        Map<String, String> response = new HashMap<>();
        response.put("code", ErrorCode.SYSTEM_ERROR.getCode());
        response.put("message", e.getMessage());
        return ResponseEntity.ok(response);
    }
}
