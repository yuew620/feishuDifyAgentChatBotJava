package com.sdxpub.feishubot.service.message.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.sdxpub.feishubot.common.constants.Constants;
import com.sdxpub.feishubot.common.exception.BotException;
import com.sdxpub.feishubot.common.exception.ErrorCode;
import com.sdxpub.feishubot.model.message.Message;
import com.sdxpub.feishubot.model.message.MessageBuffer;
import com.sdxpub.feishubot.model.message.Session;
import com.sdxpub.feishubot.service.dify.DifyService;
import com.sdxpub.feishubot.service.feishu.FeishuService;
import com.sdxpub.feishubot.service.message.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.sdxpub.feishubot.model.feishu.FeishuCard;
import com.sdxpub.feishubot.model.dify.DifyMessage;
import com.sdxpub.feishubot.service.card.CardPool;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final DifyService difyService;
    private final FeishuService feishuService;
    private final CardPool cardPool;
    private final Cache<String, Session> sessionCache;
    private final ScheduledExecutorService scheduledExecutor;
    private final ConcurrentHashMap<String, MessageBuffer> messageBuffers;
    private static final long BUFFER_FLUSH_INTERVAL_MS = 100;
    private static final long BUFFER_MAX_TIMEOUT_MS = BUFFER_FLUSH_INTERVAL_MS * 3;

    public MessageServiceImpl(DifyService difyService, 
                            FeishuService feishuService, 
                            CardPool cardPool, 
                            Cache<String, Session> sessionCache, 
                            ScheduledExecutorService scheduledExecutor) {
        this.difyService = difyService;
        this.feishuService = feishuService;
        this.cardPool = cardPool;
        this.sessionCache = sessionCache;
        this.scheduledExecutor = scheduledExecutor;
        this.messageBuffers = new ConcurrentHashMap<>();
    }

    @Override
    @Async
    public void handleMessage(Message message) {
        String userId = message.getUserId();
        String messageId = message.getMessageId();
        log.info("[MessageService] Starting to handle message - ID: {}, User: {}", messageId, userId);
        
        MessageBuffer buffer = getOrCreateBuffer(userId, messageId);
        log.info("[MessageService] Created/Retrieved message buffer for message ID: {}", messageId);

        try {
            // 并行处理卡片创建和消息发送
            log.info("[MessageService] Starting parallel processing for message ID: {}", messageId);
            CompletableFuture<Void> cardFuture = handleCardCreation(message, buffer);
            CompletableFuture<Void> difyFuture = handleDifyMessage(message);

            // 等待两个任务都完成
            CompletableFuture.allOf(cardFuture, difyFuture)
                .exceptionally(e -> {
                    log.error("Error in message handling: {}", e.getMessage(), e);
                    throw new BotException(ErrorCode.MESSAGE_HANDLE_ERROR, e.getMessage());
                });
        } catch (Exception e) {
            log.error("Failed to handle message: {}", messageId, e);
            throw new BotException(ErrorCode.MESSAGE_HANDLE_ERROR, e.getMessage());
        }
    }

    private CompletableFuture<Void> handleCardCreation(Message message, MessageBuffer buffer) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("[MessageService] Attempting to get card from pool for message ID: {}", message.getMessageId());
                FeishuCard initialCard = cardPool.getCard();
                if (initialCard == null) {
                    log.error("[MessageService] Failed to get card from pool for message ID: {}", message.getMessageId());
                    throw new BotException(ErrorCode.FEISHU_CARD_CREATE_ERROR, "Failed to get card from pool");
                }
                log.info("[MessageService] Successfully got card from pool: {}", initialCard.getCardId());

                feishuService.sendCard(message, initialCard)
                    .thenAccept(success -> {
                        if (success) {
                            buffer.setCardReady(true);
                        } else {
                            log.warn("[MessageService] Failed to send initial card, attempting retry for message ID: {}", message.getMessageId());
                            FeishuCard retryCard = cardPool.getCard();
                            if (retryCard != null) {
                                log.info("[MessageService] Retrying with new card {} for message ID: {}", retryCard.getCardId(), message.getMessageId());
                                feishuService.sendCard(message, retryCard)
                                    .thenAccept(retrySuccess -> buffer.setCardReady(retrySuccess));
                            }
                        }
                    });
            } catch (Exception e) {
                log.error("Failed to create/send card: {}", e.getMessage(), e);
                throw new BotException(ErrorCode.FEISHU_CARD_CREATE_ERROR, e.getMessage());
            }
        });
    }

    private CompletableFuture<Void> handleDifyMessage(Message message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String userId = message.getUserId();
                Session session = getSession(userId);
                
                if (session == null || session.isExpired()) {
                    difyService.createNewConversation(userId);
                }
                
                difyService.sendMessage(message, response -> 
                    handleDifyResponse(response, userId, message.getMessageId()));
            } catch (Exception e) {
                log.error("Failed to send message to Dify: {}", e.getMessage(), e);
                throw new BotException(ErrorCode.DIFY_REQUEST_ERROR, e.getMessage());
            }
        });
    }

    @Override
    public MessageBuffer getOrCreateBuffer(String userId, String messageId) {
        String key = userId + "_" + messageId;
        return messageBuffers.computeIfAbsent(key, k -> {
            MessageBuffer buffer = new MessageBuffer(userId, messageId);
            scheduleBufferTimeout(buffer);
            return buffer;
        });
    }

    @Override
    public Session getSession(String userId) {
        return sessionCache.getIfPresent(userId);
    }

    @Override
    public Session createSession(String userId, String conversationId) {
        Session session = Session.createNew(userId, userId, conversationId);
        sessionCache.put(userId, session);
        return session;
    }

    @Override
    public void updateSession(Session session) {
        session.updateLastAccessTime();
        sessionCache.put(session.getUserId(), session);
    }

    @Override
    public void removeSession(String userId) {
        sessionCache.invalidate(userId);
    }

    @Override
    public void removeBuffer(String userId, String messageId) {
        String key = userId + "_" + messageId;
        MessageBuffer buffer = messageBuffers.remove(key);
        if (buffer != null && buffer.getFlushTimer() != null) {
            buffer.getFlushTimer().cancel(false);
        }
    }

    @Override
    public void triggerMessageSend(String userId, String messageId) {
        MessageBuffer buffer = getOrCreateBuffer(userId, messageId);
        if (buffer.isEmpty() || !buffer.isCardReady()) {
            return;
        }

        if (buffer.tryLock()) {
            try {
                String content = buffer.getAndClear();
                feishuService.updateCard(messageId, content)
                    .thenAccept(success -> {
                        if (!success) {
                            log.error("Failed to update card: {}", messageId);
                        }
                    })
                    .exceptionally(e -> {
                        log.error("Error updating card: {}", messageId, e);
                        return null;
                    });
            } finally {
                buffer.unlock();
            }
        }
    }

    private void scheduleBufferTimeout(MessageBuffer buffer) {
        ScheduledFuture<?> future = scheduledExecutor.schedule(
            () -> {
                if (!buffer.isEmpty()) {
                    triggerMessageSend(buffer.getUserId(), buffer.getMessageId());
                }
                removeBuffer(buffer.getUserId(), buffer.getMessageId());
            },
            Constants.MESSAGE_BUFFER_MAX_TIMEOUT_MS,
            TimeUnit.MILLISECONDS
        );
        buffer.setFlushTimer(future);
    }

    private void handleDifyResponse(Object response, String userId, String messageId) {
        MessageBuffer buffer = getOrCreateBuffer(userId, messageId);
        
        if (response instanceof DifyMessage) {
            DifyMessage difyMessage = (DifyMessage) response;
            
            // 处理会话ID
            if (difyMessage.getConversationId() != null) {
                Session session = getSession(userId);
                if (session == null) {
                    session = createSession(userId, difyMessage.getConversationId());
                } else {
                    session.setConversationId(difyMessage.getConversationId());
                    updateSession(session);
                }
            }

            // 处理消息内容
            if (difyMessage.getAnswer() != null) {
                // Agent消息，追加到缓冲区
                buffer.append(difyMessage.getAnswer());
                
                // 检查是否需要触发发送
                if (buffer.shouldFlush()) {
                    triggerMessageSend(userId, messageId);
                }
            } else {
                // 非Agent消息，立即触发发送
                if (!buffer.isEmpty()) {
                    triggerMessageSend(userId, messageId);
                }
            }
        } else if (response instanceof String) {
            // 处理普通文本消息
            buffer.append((String) response);
            if (buffer.shouldFlush()) {
                triggerMessageSend(userId, messageId);
            }
        }
    }

    private void scheduleDelayedSend(MessageBuffer buffer) {
        if (buffer.getFlushTimer() != null) {
            buffer.getFlushTimer().cancel(false);
        }

        ScheduledFuture<?> timer = scheduledExecutor.schedule(
            () -> triggerMessageSend(buffer.getUserId(), buffer.getMessageId()),
            BUFFER_FLUSH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        buffer.setFlushTimer(timer);
    }
}
