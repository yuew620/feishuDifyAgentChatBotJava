package com.sdxpub.feishubot.service.card;

import com.sdxpub.feishubot.model.feishu.FeishuCard;
import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.service.feishu.FeishuService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private FeishuService feishuService;

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
                Message message = Message.builder()
                    .userId("system")
                    .messageId("pool-" + System.currentTimeMillis())
                    .build();
                FeishuCard card = feishuService.createCard(message).get();
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
