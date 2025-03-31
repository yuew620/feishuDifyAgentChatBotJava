package com.sdxpub.feishubot.model.message;

import java.time.LocalDateTime;

public class Session {
    private String userId;
    private String sessionId;
    private String conversationId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
    private LocalDateTime expireTime;

    public Session() {
    }

    public Session(String userId, String sessionId, String conversationId, 
                  LocalDateTime createdAt, LocalDateTime lastAccessTime, LocalDateTime expireTime) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.createdAt = createdAt;
        this.lastAccessTime = lastAccessTime;
        this.expireTime = expireTime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
        // 更新过期时间为当前时间后12小时
        this.expireTime = this.lastAccessTime.plusHours(12);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public static Session createNew(String userId, String sessionId, String conversationId) {
        LocalDateTime now = LocalDateTime.now();
        return new Session(userId, sessionId, conversationId, now, now, now.plusHours(12));
    }
}
