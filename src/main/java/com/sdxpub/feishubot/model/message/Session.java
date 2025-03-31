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
public class Session {
    private String userId;
    private String sessionId;
    private String conversationId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
    private LocalDateTime expireTime;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
        // 更新过期时间为当前时间后12小时
        this.expireTime = this.lastAccessTime.plusHours(12);
    }

    public static Session createNew(String userId, String sessionId, String conversationId) {
        LocalDateTime now = LocalDateTime.now();
        return Session.builder()
                .userId(userId)
                .sessionId(sessionId)
                .conversationId(conversationId)
                .createdAt(now)
                .lastAccessTime(now)
                .expireTime(now.plusHours(12))
                .build();
    }
}
