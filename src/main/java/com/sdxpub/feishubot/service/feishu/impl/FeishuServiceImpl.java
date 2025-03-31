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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import com.sdxpub.feishubot.model.message.Message;

@Service
public class FeishuServiceImpl implements FeishuService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeishuServiceImpl.class);
    
    private final OkHttpClient httpClient;
    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");


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

    private final CardPool cardPool;

    private class CardPool {
        private static final int INITIAL_POOL_SIZE = 20;
        private static final int MIN_POOL_SIZE = 5;
        private static final int MAX_RETRIES = 3;
        private static final long RETRY_INTERVAL = 1000; // 1 second
        
        private final ConcurrentLinkedQueue<FeishuCard> cardPool = new ConcurrentLinkedQueue<>();
        private final AtomicInteger poolSize = new AtomicInteger(0);
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        @PostConstruct
        public void init() {
            log.info("[CardPool] Initializing card pool with target size: {}", INITIAL_POOL_SIZE);
            LocalDateTime startTime = LocalDateTime.now();
            log.info("[CardPool] ===== Starting initial pool fill with size {} at {} =====", 
                    INITIAL_POOL_SIZE, 
                    startTime.format(timeFormatter));

            fillPool();

            log.info("[CardPool] ===== Initial pool fill completed at {}, took {} seconds, current size: {} =====",
                    LocalDateTime.now().format(timeFormatter),
                    java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds(),
                    poolSize.get());
        }

        @Scheduled(cron = "0 0 0 * * ?") // 每天0点
        public void rebuildPool() {
            log.info("[CardPool] Starting daily card pool rebuild at {}", 
                    LocalDateTime.now().format(timeFormatter));
            cardPool.clear();
            poolSize.set(0);
            fillPool();
        }

        private void fillPool() {
            while (true) {
                int currentSize = poolSize.get();
                if (currentSize >= INITIAL_POOL_SIZE) {
                    log.info("[CardPool] Pool filled to target size: {} at {}", 
                            INITIAL_POOL_SIZE, 
                            LocalDateTime.now().format(timeFormatter));
                    break;
                }

                LocalDateTime cardStartTime = LocalDateTime.now();
                log.info("[CardPool] >>>>> Creating card {}/{} at {}", 
                        currentSize + 1, 
                        INITIAL_POOL_SIZE, 
                        cardStartTime.format(timeFormatter));

                try {
                    createCardWithRetry();
                    log.info("[CardPool] <<<<< Card {}/{} created successfully in {} seconds",
                            currentSize + 1,
                            INITIAL_POOL_SIZE,
                            java.time.Duration.between(cardStartTime, LocalDateTime.now()).getSeconds());
                } catch (Exception e) {
                    log.error("[CardPool] !!!!! Failed to create card {}/{}: {}", 
                            currentSize + 1, 
                            INITIAL_POOL_SIZE, 
                            e.getMessage());
                    continue;
                }

                try {
                    Thread.sleep(100); // 避免创建过快
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void createCardWithRetry() throws Exception {
            Exception lastException = null;
            
            for (int i = 0; i < MAX_RETRIES; i++) {
                if (i > 0) {
                    Thread.sleep(RETRY_INTERVAL);
                }

                log.info("[CardPool] Attempting to create card (attempt {}/{}) at {}", 
                        i + 1, 
                        MAX_RETRIES, 
                        LocalDateTime.now().format(timeFormatter));
                
                try {
                    Message message = Message.createTextMessage(
                        "system",
                        "pool-" + System.currentTimeMillis(),
                        ""
                    );
                    FeishuCard card = createCard(message).get();
                    card.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24小时过期
                    
                    cardPool.offer(card);
                    poolSize.incrementAndGet();
                    
                    log.info("[CardPool] Successfully created and added new card to pool: {} at {}", 
                            card.getCardId(), 
                            LocalDateTime.now().format(timeFormatter));
                    return;
                } catch (Exception e) {
                    lastException = e;
                    log.error("[CardPool] Failed to create card (attempt {}/{}): {}", 
                            i + 1, 
                            MAX_RETRIES, 
                            e.getMessage());
                }
            }

            throw new Exception("Failed to create card after " + MAX_RETRIES + " attempts", lastException);
        }

        public FeishuCard getCard() {
            FeishuCard card = cardPool.poll();
            if (card != null) {
                poolSize.decrementAndGet();
                log.info("[CardPool] Got card from pool: {}, remaining cards: {} at {}", 
                        card.getCardId(), 
                        poolSize.get(), 
                        LocalDateTime.now().format(timeFormatter));

                // 异步创建新卡片补充到池中
                CompletableFuture.runAsync(() -> {
                    try {
                        createCardWithRetry();
                    } catch (Exception e) {
                        log.error("Failed to create replacement card: {}", e.getMessage());
                        // 继续尝试创建，避免池子逐渐缩小
                        CompletableFuture.runAsync(() -> {
                            try {
                                createCardWithRetry();
                            } catch (Exception ex) {
                                log.error("Failed to create replacement card in second attempt: {}", ex.getMessage());
                            }
                        });
                    }
                });

                return card;
            }

            // 如果没有可用卡片，同步创建一个
            log.info("[CardPool] No cards available in pool, creating new one at {}", 
                    LocalDateTime.now().format(timeFormatter));
            try {
                createCardWithRetry();
                card = cardPool.poll();
                if (card != null) {
                    poolSize.decrementAndGet();
                }
                return card;
            } catch (Exception e) {
                log.error("Failed to create card when pool is empty: {}", e.getMessage());
                return null;
            }
        }

        public int getPoolSize() {
            return poolSize.get();
        }
    }

    public FeishuServiceImpl(OkHttpClient httpClient, FeishuProperties feishuProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.feishuProperties = feishuProperties;
        this.objectMapper = objectMapper;
        this.cardPool = new CardPool();
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
