package com.sdxpub.feishubot.common.constants;

public class Constants {
    // 缓存相关常量
    public static final long SESSION_EXPIRE_HOURS = 12L;
    public static final long MESSAGE_BUFFER_TIMEOUT_MS = 100L;
    public static final long MESSAGE_BUFFER_MAX_TIMEOUT_MS = MESSAGE_BUFFER_TIMEOUT_MS * 3;
    public static final int INITIAL_BUFFER_CAPACITY = 1024;
    public static final int MAX_CACHE_SIZE = 10000;

    // 飞书相关常量
    public static final String FEISHU_EVENT_TYPE_MESSAGE = "im.message.receive_v1";
    public static final String FEISHU_MESSAGE_TYPE_TEXT = "text";
    public static final String FEISHU_CARD_STATUS_OK = "ok";
    public static final String FEISHU_CARD_STATUS_FAILED = "failed";

    // Dify相关常量
    public static final String DIFY_EVENT_AGENT_MESSAGE = "agent_message";
    public static final String DIFY_EVENT_MESSAGE = "message";
    public static final String DIFY_RESPONSE_MODE_STREAMING = "streaming";
    public static final String DIFY_HISTORY_NULL = "null";

    // HTTP相关常量
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final int HTTP_TIMEOUT_SECONDS = 30;
    public static final int HTTP_MAX_RETRIES = 3;
    public static final int HTTP_RETRY_INTERVAL_MS = 1000;

    // 响应码
    public static final String RESPONSE_CODE_SUCCESS = "0";
    public static final String RESPONSE_CODE_ERROR = "1";

    // 前缀
    public static final String SESSION_ID_PREFIX = "session_";
    public static final String MESSAGE_ID_PREFIX = "msg_";
    public static final String CARD_ID_PREFIX = "card_";

    private Constants() {
        // 私有构造函数防止实例化
    }
}
