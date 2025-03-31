package com.sdxpub.feishubot.model.message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public class MessageBuffer {
    private final String userId;
    private final String messageId;
    private final StringBuilder content;
    private final ReentrantLock lock;
    private final AtomicBoolean cardReady;
    private final AtomicLong lastSendTime;
    private ScheduledFuture<?> flushTimer;

    public void setFlushTimer(ScheduledFuture<?> flushTimer) {
        this.flushTimer = flushTimer;
    }

    public ScheduledFuture<?> getFlushTimer() {
        return flushTimer;
    }

    public MessageBuffer(String userId, String messageId) {
        this.userId = userId;
        this.messageId = messageId;
        this.content = new StringBuilder();
        this.lock = new ReentrantLock();
        this.cardReady = new AtomicBoolean(false);
        this.lastSendTime = new AtomicLong(0);
    }

    public void append(String text) {
        if (lock.tryLock()) {
            try {
                content.append(text);
            } finally {
                lock.unlock();
            }
        }
    }

    public String getAndClear() {
        if (lock.tryLock()) {
            try {
                String result = content.toString();
                content.setLength(0);
                lastSendTime.set(System.currentTimeMillis());
                return result;
            } finally {
                lock.unlock();
            }
        }
        return "";
    }

    public boolean isEmpty() {
        if (lock.tryLock()) {
            try {
                return content.length() == 0;
            } finally {
                lock.unlock();
            }
        }
        return true;
    }

    public boolean shouldFlush() {
        long now = System.currentTimeMillis();
        return !isEmpty() && 
               cardReady.get() && 
               (now - lastSendTime.get() >= 100);  // 100ms发送间隔
    }

    public boolean tryLock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void setCardReady(boolean ready) {
        cardReady.set(ready);
    }

    public boolean isCardReady() {
        return cardReady.get();
    }

    public void resetFlushTimer() {
        if (flushTimer != null) {
            flushTimer.cancel(false);
            flushTimer = null;
        }
    }
}
