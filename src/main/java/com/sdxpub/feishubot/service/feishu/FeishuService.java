package com.sdxpub.feishubot.service.feishu;

import com.sdxpub.feishubot.model.feishu.FeishuCard;
import com.sdxpub.feishubot.model.message.Message;

import java.util.concurrent.CompletableFuture;

public interface FeishuService {
    /**
     * 创建空白卡片（用于预创建）
     */
    CompletableFuture<FeishuCard> createCard(Message message);

    /**
     * 发送卡片给用户
     */
    CompletableFuture<Boolean> sendCard(Message message, FeishuCard card);

    /**
     * 更新卡片内容
     */
    CompletableFuture<Boolean> updateCard(String cardId, String content);

    /**
     * 获取卡片信息
     */
    FeishuCard getCard(String userId, String messageId);

    /**
     * 检查卡片是否已准备好
     */
    boolean isCardReady(String userId, String messageId);

    /**
     * 设置卡片状态为准备好
     */
    void setCardReady(String userId, String messageId, String cardId);

    /**
     * 设置卡片状态为失败
     */
    void setCardFailed(String userId, String messageId);

    /**
     * 移除卡片
     */
    void removeCard(String userId, String messageId);
}
