package com.sdxpub.feishubot.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 系统错误: 1000-1999
    SYSTEM_ERROR("1000", "系统错误"),
    CONFIG_ERROR("1001", "配置错误"),
    INITIALIZATION_ERROR("1002", "初始化错误"),
    
    // 飞书相关错误: 2000-2999
    FEISHU_API_ERROR("2000", "飞书API调用错误"),
    FEISHU_AUTH_ERROR("2001", "飞书认证错误"),
    FEISHU_CARD_CREATE_ERROR("2002", "飞书卡片创建错误"),
    FEISHU_CARD_UPDATE_ERROR("2003", "飞书卡片更新错误"),
    
    // Dify相关错误: 3000-3999
    DIFY_API_ERROR("3000", "Dify API调用错误"),
    DIFY_AUTH_ERROR("3001", "Dify认证错误"),
    DIFY_REQUEST_ERROR("3002", "Dify请求错误"),
    DIFY_RESPONSE_ERROR("3003", "Dify响应错误"),
    
    // 消息处理错误: 4000-4999
    MESSAGE_PARSE_ERROR("4000", "消息解析错误"),
    MESSAGE_HANDLE_ERROR("4001", "消息处理错误"),
    MESSAGE_SEND_ERROR("4002", "消息发送错误"),
    MESSAGE_BUFFER_ERROR("4003", "消息缓冲区错误"),
    
    // 会话相关错误: 5000-5999
    SESSION_NOT_FOUND("5000", "会话不存在"),
    SESSION_EXPIRED("5001", "会话已过期"),
    SESSION_CREATE_ERROR("5002", "会话创建错误"),
    
    // 参数验证错误: 6000-6999
    PARAM_INVALID("6000", "参数无效"),
    PARAM_MISSING("6001", "参数缺失"),
    
    // 缓存相关错误: 7000-7999
    CACHE_ERROR("7000", "缓存操作错误"),
    CACHE_KEY_NOT_FOUND("7001", "缓存键不存在"),
    CACHE_VALUE_INVALID("7002", "缓存值无效");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
