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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
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
                    .url("http://dify.sdx.pub/v1/chat-messages")
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
        MediaType mediaType = MediaType.parse("application/json");
        String jsonBody = String.format(
            "{\"inputs\":{\"history\":\"null\"},\"query\":\"%s\",\"user\":\"%s\",\"response_mode\":\"streaming\"%s}",
            request.getQuery().replace("\"", "\\\""),
            request.getUser(),
            request.getConversationId() != null ? ",\"conversation_id\":\"" + request.getConversationId() + "\"" : ""
        );
        return RequestBody.create(jsonBody, mediaType);
    }

    private static final long SEND_INTERVAL = 100; // 发送间隔100ms
    private static final long BUFFER_TIMEOUT = SEND_INTERVAL * 3; // 缓冲区超时时间
    private final Map<String, Long> lastSendTimeMap = new ConcurrentHashMap<>();
    private final Map<String, Timer> bufferTimers = new ConcurrentHashMap<>();

    private void sendBufferedMessage(String userId, StringBuilder messageBuffer, Consumer<DifyResponse> onResponse, String conversationId) {
        long currentTime = System.currentTimeMillis();
        Long lastSendTime = lastSendTimeMap.get(userId);
        
        if (lastSendTime == null || currentTime - lastSendTime >= SEND_INTERVAL) {
            DifyResponse bufferedResponse = new DifyResponse(
                "agent_message",  // event
                null,            // task
                null,            // id
                messageBuffer.toString(), // answer
                conversationId,  // conversationId
                null,            // usage
                null             // message
            );
            onResponse.accept(bufferedResponse);
            messageBuffer.setLength(0);
            lastSendTimeMap.put(userId, currentTime);
        }
    }

    private void resetBufferTimer(String userId, StringBuilder messageBuffer, Consumer<DifyResponse> onResponse, String conversationId) {
        Timer oldTimer = bufferTimers.get(userId);
        if (oldTimer != null) {
            oldTimer.cancel();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (messageBuffer.length() > 0) {
                    sendBufferedMessage(userId, messageBuffer, onResponse, conversationId);
                }
                timer.cancel();
                bufferTimers.remove(userId);
            }
        }, BUFFER_TIMEOUT);
        
        bufferTimers.put(userId, timer);
    }

    private void handleStreamingResponse(Response response, String userId, Consumer<DifyResponse> onResponse) {
        try (ResponseBody responseBody = response.body()) {
            if (responseBody == null) {
                throw new BotException(ErrorCode.DIFY_API_ERROR, "Empty response body");
            }

            StringBuilder messageBuffer = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    DifyResponse difyResponse = DifyResponse.fromJson(jsonData);
                    handleResponse(difyResponse, userId);

                    if ("agent_message".equals(difyResponse.getEvent())) {
                        if (difyResponse.getAnswer() != null && !difyResponse.getAnswer().isEmpty()) {
                            messageBuffer.append(difyResponse.getAnswer());
                            resetBufferTimer(userId, messageBuffer, onResponse, difyResponse.getConversationId());
                        }
                    } else {
                        // 非agent_message消息触发发送
                        if (messageBuffer.length() > 0) {
                            sendBufferedMessage(userId, messageBuffer, onResponse, difyResponse.getConversationId());
                        }
                        
                        if ("message_end".equals(difyResponse.getEvent())) {
                            Timer timer = bufferTimers.remove(userId);
                            if (timer != null) {
                                timer.cancel();
                            }
                            removeConversation(userId);
                            lastSendTimeMap.remove(userId);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new BotException(ErrorCode.DIFY_API_ERROR, "Error reading streaming response: " + e.getMessage());
        }
    }
}
