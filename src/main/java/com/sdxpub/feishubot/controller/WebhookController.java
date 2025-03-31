package com.sdxpub.feishubot.controller;

import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final MessageService messageService;

    @PostMapping("/event")
    public ResponseEntity<Map<String, String>> handleEvent(@RequestBody Map<String, Object> payload) {
        log.debug("Received event webhook: {}", payload);
        
        // 处理飞书的challenge验证
        if (payload.containsKey("challenge")) {
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge").toString()));
        }

        try {
            // 从payload中提取消息内容
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String messageId = event.get("message_id").toString();
            String userId = event.get("sender").toString();
            String content = event.get("content").toString();

            // 创建消息对象
            Message message = Message.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .content(content)
                    .type(Message.MessageType.TEXT)
                    .build();

            // 异步处理消息
            messageService.handleMessage(message);

            return ResponseEntity.ok(Map.of("code", "0"));
        } catch (Exception e) {
            log.error("Failed to handle event", e);
            return ResponseEntity.ok(Map.of("code", "1", "message", e.getMessage()));
        }
    }

    @PostMapping("/card")
    public ResponseEntity<Map<String, String>> handleCard(@RequestBody Map<String, Object> payload) {
        log.debug("Received card webhook: {}", payload);
        return ResponseEntity.ok(Map.of("code", "0"));
    }
}
