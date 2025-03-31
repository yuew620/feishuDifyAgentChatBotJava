package com.sdxpub.feishubot.model.feishu;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeishuCard {
    private String cardId;
    private String userId;
    private String messageId;
    private String content;
    private CardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdateTime;

    public enum CardStatus {
        CREATING,    // 卡片正在创建中
        READY,      // 卡片已创建完成
        UPDATING,   // 卡片正在更新中
        FAILED      // 卡片创建或更新失败
    }

    public static FeishuCard createNew(String userId, String messageId) {
        LocalDateTime now = LocalDateTime.now();
        return FeishuCard.builder()
                .userId(userId)
                .messageId(messageId)
                .content("")
                .status(CardStatus.CREATING)
                .createdAt(now)
                .lastUpdateTime(now)
                .build();
    }

    public void updateContent(String newContent) {
        this.content = newContent;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void setReady(String cardId) {
        this.cardId = cardId;
        this.status = CardStatus.READY;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void setFailed() {
        this.status = CardStatus.FAILED;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public boolean isReady() {
        return this.status == CardStatus.READY;
    }
}
