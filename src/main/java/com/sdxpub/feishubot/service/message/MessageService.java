package com.sdxpub.feishubot.service.message;

import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.model.message.MessageBuffer;
import com.sdxpub.feishubot.model.message.Session;

public interface MessageService {
    /**
     * 处理接收到的消息
     */
    void handleMessage(Message message);

    /**
     * 获取或创建消息缓冲区
     */
    MessageBuffer getOrCreateBuffer(String userId, String messageId);

    /**
     * 获取会话信息
     */
    Session getSession(String userId);

    /**
     * 创建新会话
     */
    Session createSession(String userId, String conversationId);

    /**
     * 更新会话
     */
    void updateSession(Session session);

    /**
     * 移除会话
     */
    void removeSession(String userId);

    /**
     * 移除消息缓冲区
     */
    void removeBuffer(String userId, String messageId);

    /**
     * 触发消息发送
     */
    void triggerMessageSend(String userId, String messageId);
}
