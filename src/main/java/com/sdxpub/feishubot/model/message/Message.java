package com.sdxpub.feishubot.model.message;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String userId;
    private String messageId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        UNKNOWN
    }
}
