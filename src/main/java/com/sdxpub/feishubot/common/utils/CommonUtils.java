package com.sdxpub.feishubot.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
public class CommonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成唯一ID
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 计算两个时间之间的毫秒差
     */
    public static long getMillisBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MILLIS.between(start, end);
    }

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON", e);
            return "{}";
        }
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to object", e);
            return null;
        }
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 安全地获取Map中的字符串值
     */
    public static String getStringFromMap(Object value) {
        return value != null ? value.toString() : "";
    }

    /**
     * 生成会话ID
     */
    public static String generateSessionId(String userId) {
        return String.format("session_%s_%s", userId, generateUniqueId());
    }

    /**
     * 生成消息ID
     */
    public static String generateMessageId(String userId) {
        return String.format("msg_%s_%s", userId, generateUniqueId());
    }
}
