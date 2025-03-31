package com.sdxpub.feishubot.service.dify.impl;

import com.sdxpub.feishubot.common.exception.BotException;
import com.sdxpub.feishubot.common.exception.ErrorCode;
import com.sdxpub.feishubot.config.DifyProperties;
import com.sdxpub.feishubot.model.dify.DifyMessage;
import com.sdxpub.feishubot.model.dify.DifyRequest;
import com.sdxpub.feishubot.model.dify.DifyResponse;
import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.dify.DifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifyServiceImpl implements DifyService {

    private final DifyProperties difyProperties;
    private final OkHttpClient httpClient;
    private final Map<String, String> conversationCache = new ConcurrentHashMap<>();

    @Override
    public void sendMessage(Message message, Consumer<DifyResponse> onResponse) {
        String userId = message.getUserId();
        String conversationId = getConversationId(userId);

        DifyRequest request = conversationId != null ?
                DifyRequest.createRequest(userId, message.getContent(), conversationId) :
                DifyRequest.createInitialRequest(userId, message.getContent());

        sendRequest(request, onResponse);
    }

    @Override
    public void sendRequest(DifyRequest request, Consumer<DifyResponse> onResponse) {
        try {
            RequestBody requestBody = createRequestBody(request);
            Request httpRequest = new Request.Builder()
                    .url(difyProperties.getApiEndpoint() + "/chat-messages")
                    .addHeader("Authorization", "Bearer " + difyProperties.getApiKey())
                    .post(requestBody)
                    .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("Failed to send request to Dify: {}", e.getMessage());
                    throw new BotException(ErrorCode.DIFY_API_ERROR, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String error = response.body() != null ? response.body().string() : "Unknown error";
                            throw new BotException(ErrorCode.DIFY_API_ERROR, error);
                        }

                        // 处理流式响应
                        handleStreamingResponse(response, request.getUser(), onResponse);
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error sending request to Dify: {}", e.getMessage());
            throw new BotException(ErrorCode.DIFY_REQUEST_ERROR, e.getMessage());
        }
    }

    @Override
    public void handleResponse(DifyResponse response, String userId) {
        if (response.getConversationId() != null) {
            conversationCache.put(userId, response.getConversationId());
        }
    }

    @Override
    public void handleDifyMessage(DifyMessage difyMessage) {
        if (difyMessage.getConversationId() != null) {
            conversationCache.put(difyMessage.getUserId(), difyMessage.getConversationId());
        }
    }

    @Override
    public String getConversationId(String userId) {
        return conversationCache.get(userId);
    }

    @Override
    public void createNewConversation(String userId) {
        conversationCache.remove(userId);
    }

    @Override
    public void removeConversation(String userId) {
        conversationCache.remove(userId);
    }

    @Override
    public boolean hasConversation(String userId) {
        return conversationCache.containsKey(userId);
    }

    private RequestBody createRequestBody(DifyRequest request) {
        // 实现请求体创建逻辑
        return null; // TODO: 实现具体的请求体创建逻辑
    }

    private void handleStreamingResponse(Response response, String userId, Consumer<DifyResponse> onResponse) {
        // 实现流式响应处理逻辑
        // TODO: 实现具体的流式响应处理逻辑
    }
}
