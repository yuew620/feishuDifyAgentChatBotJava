package com.sdxpub.feishubot.model.message;

import java.time.LocalDateTime;

public class Message {
    private String userId;
    private String messageId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;

    public Message() {
    }

    public Message(String userId, String messageId, String content, MessageType type, LocalDateTime timestamp) {
        this.userId = userId;
        this.messageId = messageId;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static Message createTextMessage(String userId, String messageId, String content) {
        return new Message(userId, messageId, content, MessageType.TEXT, LocalDateTime.now());
    }
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        UNKNOWN
    }
}
