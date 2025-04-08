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
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.card.CardPool;

@Service
public class FeishuServiceImpl implements FeishuService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeishuServiceImpl.class);
    
    private final OkHttpClient httpClient;
    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


    private final Map<String, Map<String, FeishuCard>> cardCache = new ConcurrentHashMap<>();

    @Autowired
    private CardPool cardPool;

    @Override
    public CompletableFuture<FeishuCard> createCard(Message message) {
        return cardPool.createCardForMessage(message).thenApply(card -> {
            // 缓存卡片信息
            cardCache.computeIfAbsent(message.getUserId(), k -> new ConcurrentHashMap<>())
                    .put(message.getMessageId(), card);
            return card;
        });
    }

    public FeishuServiceImpl(OkHttpClient httpClient, FeishuProperties feishuProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.feishuProperties = feishuProperties;
        this.objectMapper = objectMapper;
    }

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
                messageBody.put("receive_id_type", "open_id");
                messageBody.put("receive_id", message.getUserId());
                messageBody.put("msg_type", "interactive");
                messageBody.put("content", String.format(
                    "{\"type\":\"card\",\"data\":{\"card_id\":\"%s\"}}", 
                    finalCard.getCardId()
                ));

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

}
