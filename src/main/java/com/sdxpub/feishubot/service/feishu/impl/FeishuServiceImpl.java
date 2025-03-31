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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.card.CardPool;

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

    private final Map<String, Map<String, FeishuCard>> cardCache = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<FeishuCard> createCard(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Instant startTime = Instant.now();
                log.info("[CardCreator] Starting card entity creation at {}", 
                        LocalDateTime.now().format(timeFormatter));

                String url = feishuProperties.getApiEndpoint() + "/cardkit/v1/cards";
        
                // 构建卡片实体
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), 
                    buildCardContent()
                );

                String token = getAccessToken();
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                        log.error("[CardCreator] Failed to create card: {}", errorMsg);
                        throw new Exception("Failed to create card: " + errorMsg);
                    }

                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    String cardId = (String) responseMap.get("card_id");
                    
                    FeishuCard card = new FeishuCard();
                    card.setCardId(cardId);
                    
                    // 缓存卡片信息
                    cardCache.computeIfAbsent(message.getUserId(), k -> new ConcurrentHashMap<>())
                            .put(message.getMessageId(), card);
                    
                    return card;
                }
            } catch (Exception e) {
                log.error("[CardCreator] Error creating card: {}", e.getMessage());
                throw new RuntimeException("Error creating card", e);
            }
        });
    }

    @Autowired
    private CardPool cardPool;

    @Override
    public CompletableFuture<Boolean> sendCard(Message message, FeishuCard card) {
        final FeishuCard finalCard = card.isExpired() ? cardPool.getCard() : card;
        if (finalCard == null) {
            return CompletableFuture.completedFuture(false);
        }

        if (card != finalCard) {
            log.info("[CardSender] Card {} is expired, using new card", card.getCardId());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = feishuProperties.getApiEndpoint() + "/im/v1/messages";
                
                Map<String, Object> messageBody = new HashMap<>();
                messageBody.put("receive_id_type", "chat_id");
                messageBody.put("msg_type", "interactive");
                messageBody.put("content", String.format(
                    "{\"type\":\"card\",\"data\":{\"card_id\":\"%s\"}}", 
                    finalCard.getCardId()
                ));
                messageBody.put("receive_id", feishuProperties.getChatId());

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(messageBody)
                );

                String token = getAccessToken();
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    boolean success = response.isSuccessful();
                    if (!success) {
                        // 如果发送失败，可能是卡片失效，尝试获取新卡片重试
                        FeishuCard newCard = cardPool.getCard();
                        if (newCard != null) {
                            return sendCard(message, newCard).get();
                        }
                    }
                    return success;
                }
            } catch (Exception e) {
                log.error("[CardSender] Error sending card: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateCard(String cardId, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(
                    "%s/cardkit/v1/cards/%s/elements/markdown_1/content",
                    feishuProperties.getApiEndpoint(),
                    cardId
                );

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("content", content);
                requestBody.put("sequence", System.currentTimeMillis());

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(requestBody)
                );

                String token = getAccessToken();
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .put(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                log.error("[CardUpdater] Error updating card: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public FeishuCard getCard(String userId, String messageId) {
        Map<String, FeishuCard> userCards = cardCache.get(userId);
        return userCards != null ? userCards.get(messageId) : null;
    }

    @Override
    public boolean isCardReady(String userId, String messageId) {
        FeishuCard card = getCard(userId, messageId);
        return card != null && card.isReady();
    }

    @Override
    public void setCardReady(String userId, String messageId, String cardId) {
        cardCache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfPresent(messageId, (k, card) -> {
                    card.setReady(cardId);
                    return card;
                });
    }

    @Override
    public void setCardFailed(String userId, String messageId) {
        cardCache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfPresent(messageId, (k, card) -> {
                    card.setFailed();
                    return card;
                });
    }

    @Override
    public void removeCard(String userId, String messageId) {
        Map<String, FeishuCard> userCards = cardCache.get(userId);
        if (userCards != null) {
            userCards.remove(messageId);
            if (userCards.isEmpty()) {
                cardCache.remove(userId);
            }
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
        
        // 基本配置
        Map<String, Object> config = new HashMap<>();
        config.put("wide_screen_mode", true);
        config.put("streaming_mode", true);
        config.put("summary", new HashMap<String, String>() {{
            put("content", "[生成中]");
        }});
        
        // 流式更新配置
        Map<String, Object> streamingConfig = new HashMap<>();
        Map<String, Integer> printFrequency = new HashMap<>();
        printFrequency.put("default", 30);
        printFrequency.put("android", 25);
        printFrequency.put("ios", 40);
        printFrequency.put("pc", 50);
        streamingConfig.put("print_frequency_ms", printFrequency);
        
        Map<String, Integer> printStep = new HashMap<>();
        printStep.put("default", 2);
        printStep.put("android", 3);
        printStep.put("ios", 4);
        printStep.put("pc", 5);
        streamingConfig.put("print_step", printStep);
        streamingConfig.put("print_strategy", "fast");
        
        config.put("streaming_config", streamingConfig);
        card.put("config", config);
        
        // 卡片结构
        card.put("schema", "2.0");
        card.put("header", new HashMap<String, Object>() {{
            put("title", new HashMap<String, String>() {{
                put("content", "AI助手");
                put("tag", "plain_text");
            }});
        }});
        
        card.put("body", new HashMap<String, Object>() {{
            put("elements", new Object[]{
                new HashMap<String, String>() {{
                    put("tag", "markdown");
                    put("content", "");
                    put("element_id", "markdown_1");
                }}
            });
        }});

        return objectMapper.writeValueAsString(new HashMap<String, Object>() {{
            put("type", "card_json");
            put("data", objectMapper.writeValueAsString(card));
        }});
    }
}
