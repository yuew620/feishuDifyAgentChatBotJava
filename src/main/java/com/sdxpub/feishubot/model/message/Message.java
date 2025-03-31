package com.sdxpub.feishubot.model.message;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    public static Message createTextMessage(String userId, String messageId, String content) {
        Message message = new Message();
        message.userId = userId;
        message.messageId = messageId;
        message.content = content;
        message.type = MessageType.TEXT;
        message.timestamp = LocalDateTime.now();
        return message;
    }
    String userId;
    String messageId;
    String content;
    MessageType type;
    LocalDateTime timestamp;
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        UNKNOWN
    }
}
