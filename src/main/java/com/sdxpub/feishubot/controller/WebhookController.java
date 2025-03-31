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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebhookController.class);

    private final MessageService messageService;

    @PostMapping("/event")
    public ResponseEntity<Map<String, String>> handleEvent(@RequestBody Map<String, Object> payload) {
        log.debug("Received event webhook: {}", payload);
        
        // 处理飞书的challenge验证
        if (payload.containsKey("challenge")) {
            Map<String, String> response = new HashMap<>();
            response.put("challenge", payload.get("challenge").toString());
            return ResponseEntity.ok(response);
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

            Map<String, String> response = new HashMap<>();
            response.put("code", "0");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle event", e);
            Map<String, String> response = new HashMap<>();
            response.put("code", "1");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/card")
    public ResponseEntity<Map<String, String>> handleCard(@RequestBody Map<String, Object> payload) {
        log.debug("Received card webhook: {}", payload);
        Map<String, String> response = new HashMap<>();
        response.put("code", "0");
        return ResponseEntity.ok(response);
    }
}
