package com.sdxpub.feishubot.controller;

import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final MessageService messageService;

    @Autowired
    public WebhookController(MessageService messageService) {
        this.messageService = messageService;
    }

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
            Message message = extractMessageFromPayload(payload);
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

    private Message extractMessageFromPayload(Map<String, Object> payload) {
        // 从payload中提取消息内容
        Map<String, Object> event = (Map<String, Object>) payload.get("event");
        if (event == null) {
            throw new IllegalArgumentException("Event data is missing");
        }

        // 获取消息ID
        Object messageIdObj = event.get("message_id");
        if (messageIdObj == null) {
            throw new IllegalArgumentException("Message ID is missing");
        }
        String messageId = messageIdObj.toString();

        // 获取发送者ID
        Object senderObj = event.get("sender");
        if (senderObj == null) {
            throw new IllegalArgumentException("Sender information is missing");
        }
        Map<String, Object> sender = (Map<String, Object>) senderObj;
        String userId = sender.get("sender_id").toString();

        // 获取消息内容
        Object messageObj = event.get("message");
        if (messageObj == null) {
            throw new IllegalArgumentException("Message content is missing");
        }
        Map<String, Object> messageData = (Map<String, Object>) messageObj;
        String content = messageData.get("content").toString();
        
        log.info("[Webhook] Received message - ID: {}, User: {}, Content: {}", messageId, userId, content);

        // 创建消息对象
        Message message = Message.createTextMessage(userId, messageId, content);
        log.info("[Webhook] Created Message object: {}", message);
        
        return message;
    }

    @PostMapping("/card")
    public ResponseEntity<Map<String, String>> handleCard(@RequestBody Map<String, Object> payload) {
        log.debug("Received card webhook: {}", payload);
        Map<String, String> response = new HashMap<>();
        response.put("code", "0");
        return ResponseEntity.ok(response);
    }
}
