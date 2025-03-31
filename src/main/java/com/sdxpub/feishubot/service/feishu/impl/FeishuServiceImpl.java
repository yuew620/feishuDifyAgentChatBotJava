package com.sdxpub.feishubot.service.feishu.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdxpub.feishubot.config.FeishuProperties;
import com.sdxpub.feishubot.model.feishu.FeishuCard;
import com.sdxpub.feishubot.service.feishu.FeishuService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FeishuServiceImpl implements FeishuService {
    
    private final OkHttpClient httpClient;
    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    public FeishuServiceImpl(OkHttpClient httpClient, FeishuProperties feishuProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.feishuProperties = feishuProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public FeishuCard createCard() throws Exception {
        Instant startTime = Instant.now();
        log.info("[CardCreator] Starting card entity creation at {}", 
                LocalDateTime.now().format(timeFormatter));

        String url = feishuProperties.getApiEndpoint() + "/im/v1/messages";
        
        // 构建消息内容
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("receive_id_type", "chat_id");
        messageBody.put("msg_type", "interactive");
        messageBody.put("content", buildCardContent());

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            objectMapper.writeValueAsString(messageBody)
        );

        // 记录token获取时间
        Instant tokenStartTime = Instant.now();
        String token = getAccessToken();
        Duration tokenTime = Duration.between(tokenStartTime, Instant.now());
        log.info("[CardCreator] Token fetch took: {} ms at {}", 
                tokenTime.toMillis(), 
                LocalDateTime.now().format(timeFormatter));

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + token)
            .post(body)
            .build();

        log.info("[CardCreator] Creating card entity with URL: {} at {}", 
                url, 
                LocalDateTime.now().format(timeFormatter));

        try (Response response = httpClient.newCall(request).execute()) {
            // 记录API请求时间
            Duration apiTime = Duration.between(tokenStartTime.plus(tokenTime), Instant.now());
            log.info("[CardCreator] Card entity API request took: {} ms at {}", 
                    apiTime.toMillis(), 
                    LocalDateTime.now().format(timeFormatter));

            if (!response.isSuccessful() || response.body() == null) {
                String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                log.error("[CardCreator] Failed to create card: {}", errorMsg);
                throw new Exception("Failed to create card: " + errorMsg);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            
            if (data == null || !data.containsKey("message_id")) {
                throw new Exception("Failed to get message ID from response");
            }

            String messageId = (String) data.get("message_id");
            log.info("[CardCreator] Successfully created card entity with ID: {} at {}", 
                    messageId, 
                    LocalDateTime.now().format(timeFormatter));

            // 记录总时间
            Duration totalTime = Duration.between(startTime, Instant.now());
            log.info("[CardCreator] Total card entity creation took: {} ms at {}", 
                    totalTime.toMillis(), 
                    LocalDateTime.now().format(timeFormatter));

            FeishuCard card = new FeishuCard();
            card.setCardId(messageId);
            return card;
        } catch (IOException e) {
            log.error("[CardCreator] Error creating card: {}", e.getMessage());
            throw new Exception("Error creating card", e);
        }
    }

    private String getAccessToken() throws Exception {
        String url = feishuProperties.getApiEndpoint() + "/auth/v3/tenant_access_token/internal";
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("app_id", feishuProperties.getAppId());
        requestBody.put("app_secret", feishuProperties.getAppSecret());

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            objectMapper.writeValueAsString(requestBody)
        );

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new Exception("Failed to get access token");
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            return (String) responseMap.get("tenant_access_token");
        }
    }

    private String buildCardContent() throws Exception {
        Map<String, Object> card = new HashMap<>();
        card.put("config", new HashMap<String, Object>() {{
            put("wide_screen_mode", true);
        }});
        
        // 添加其他卡片内容配置
        // TODO: 根据实际需求配置卡片内容

        return objectMapper.writeValueAsString(new HashMap<String, Object>() {{
            put("card", card);
        }});
    }
}
