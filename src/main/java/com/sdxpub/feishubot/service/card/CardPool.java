package com.sdxpub.feishubot.service.card;

import com.sdxpub.feishubot.model.feishu.FeishuCard;
import com.sdxpub.feishubot.model.message.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdxpub.feishubot.config.FeishuProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CardPool {
    private static final Logger log = LoggerFactory.getLogger(CardPool.class);
    private static final int INITIAL_POOL_SIZE = 20;
    private static final int MIN_POOL_SIZE = 5;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INTERVAL = 1000; // 1 second
    
    private final ConcurrentLinkedQueue<FeishuCard> cardPool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OkHttpClient httpClient;
    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;
    
    public CardPool(OkHttpClient httpClient, FeishuProperties feishuProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.feishuProperties = feishuProperties;
        this.objectMapper = objectMapper;
    }

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
                FeishuCard card = createCard();
                card.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24小时过期
                
                cardPool.offer(card);
                poolSize.incrementAndGet();
                
                log.info("[CardPool] Successfully created and added new card to pool - ID: {}, Status: {}, at {}", 
                        card.getCardId(),
                        card.isReady() ? "READY" : "NOT_READY",
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
    
    public CompletableFuture<FeishuCard> createCardForMessage(Message message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 直接创建消息卡片
                String url = feishuProperties.getApiEndpoint() + "/cardkit/v1/cards";
                
                // 构建卡片实体
                Map<String, Object> cardData = new HashMap<>();
                cardData.put("schema", "2.0");
                
                // Header
                Map<String, Object> header = new HashMap<>();
                Map<String, Object> title = new HashMap<>();
                title.put("content", "AI助手");
                title.put("tag", "plain_text");
                header.put("title", title);
                cardData.put("header", header);
                
                // Config
                Map<String, Object> config = new HashMap<>();
                config.put("streaming_mode", true);
                Map<String, String> summary = new HashMap<>();
                summary.put("content", "[生成中]");
                config.put("summary", summary);
                cardData.put("config", config);
                
                // Body
                Map<String, Object> bodyContent = new HashMap<>();
                List<Map<String, String>> elements = new ArrayList<>();
                Map<String, String> markdown = new HashMap<>();
                markdown.put("tag", "markdown");
                markdown.put("content", "");
                markdown.put("element_id", "markdown_1");
                elements.add(markdown);
                bodyContent.put("elements", elements);
                cardData.put("body", bodyContent);

                Map<String, Object> request = new HashMap<>();
                request.put("type", "card_json");
                request.put("data", objectMapper.writeValueAsString(cardData));

                RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    objectMapper.writeValueAsString(request)
                );

                String token = getAccessToken();
                Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(requestBody)
                    .build();

                Response response = httpClient.newCall(httpRequest).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("[CardCreator] Failed to create card: {}", errorMsg);
                    throw new Exception("Failed to create card: " + errorMsg);
                }

                String responseBody = response.body().string();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                String cardId = (String) responseMap.get("card_id");
                
                FeishuCard messageCard = FeishuCard.createNew(message.getUserId(), message.getMessageId());
                messageCard.setReady(cardId);
                messageCard.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24小时过期
                
                log.info("[CardCreator] Created card for message - ID: {}, User: {}, Message: {}", 
                        cardId, message.getUserId(), message.getMessageId());
                
                return messageCard;
            } catch (Exception e) {
                log.error("[CardPool] Error creating card for message: {}", e.getMessage());
                throw new RuntimeException("Error creating card for message", e);
            }
        });
    }

    private FeishuCard createCard() throws Exception {
        String url = feishuProperties.getApiEndpoint() + "/cardkit/v1/cards";
        
        // 构建卡片实体
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("schema", "2.0");
        
        // Header
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> title = new HashMap<>();
        title.put("content", "AI助手");
        title.put("tag", "plain_text");
        header.put("title", title);
        cardData.put("header", header);
        
        // Config
        Map<String, Object> config = new HashMap<>();
        config.put("streaming_mode", true);
        Map<String, String> summary = new HashMap<>();
        summary.put("content", "[生成中]");
        config.put("summary", summary);
        cardData.put("config", config);
        
        // Body
        Map<String, Object> bodyContent = new HashMap<>();
        List<Map<String, String>> elements = new ArrayList<>();
        Map<String, String> markdown = new HashMap<>();
        markdown.put("tag", "markdown");
        markdown.put("content", "");
        markdown.put("element_id", "markdown_1");
        elements.add(markdown);
        bodyContent.put("elements", elements);
        cardData.put("body", bodyContent);

        Map<String, Object> request = new HashMap<>();
        request.put("type", "card_json");
        request.put("data", objectMapper.writeValueAsString(cardData));

        RequestBody requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            objectMapper.writeValueAsString(request)
        );

        String token = getAccessToken();
        Request httpRequest = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + token)
            .post(requestBody)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                log.error("[CardCreator] Failed to create card: {}", errorMsg);
                throw new Exception("Failed to create card: " + errorMsg);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            String cardId = (String) responseMap.get("card_id");
            
            FeishuCard card = FeishuCard.createNew(
                "system",
                "pool-" + System.currentTimeMillis()
            );
            card.setReady(cardId);
            
            return card;
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
