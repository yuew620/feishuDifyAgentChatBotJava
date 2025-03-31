package com.sdxpub.feishubot.service.dify;

import com.sdxpub.feishubot.model.dify.DifyMessage;
import com.sdxpub.feishubot.model.dify.DifyRequest;
import com.sdxpub.feishubot.model.dify.DifyResponse;
import com.sdxpub.feishubot.model.message.Message;

import java.util.function.Consumer;

public interface DifyService {
    /**
     * 发送消息到Dify
     */
    void sendMessage(Message message, Consumer<DifyResponse> onResponse);

    /**
     * 发送请求到Dify
     */
    void sendRequest(DifyRequest request, Consumer<DifyResponse> onResponse);

    /**
     * 处理Dify响应
     */
    void handleResponse(DifyResponse response, String userId);

    /**
     * 处理Dify消息
     */
    void handleDifyMessage(DifyMessage difyMessage);

    /**
     * 获取会话ID
     */
    String getConversationId(String userId);

    /**
     * 创建新会话
     */
    void createNewConversation(String userId);

    /**
     * 移除会话
     */
    void removeConversation(String userId);

    /**
     * 检查会话是否存在
     */
    boolean hasConversation(String userId);
}
