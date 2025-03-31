package com.sdxpub.feishubot.model.feishu;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeishuCard {
    String cardId;
    String userId;
    String messageId;
    String content;
    CardStatus status;
    LocalDateTime createdAt;
    LocalDateTime lastUpdateTime;
    long expireTime;

    public enum CardStatus {
        CREATING,    // 卡片正在创建中
        READY,      // 卡片已创建完成
        UPDATING,   // 卡片正在更新中
        FAILED      // 卡片创建或更新失败
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
    
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
    
    public String getCardId() {
        return cardId;
    }

    public static FeishuCard createNew(String userId, String messageId) {
        LocalDateTime now = LocalDateTime.now();
        FeishuCard card = new FeishuCard();
        card.userId = userId;
        card.messageId = messageId;
        card.content = "";
        card.status = CardStatus.CREATING;
        card.createdAt = now;
        card.lastUpdateTime = now;
        return card;
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
