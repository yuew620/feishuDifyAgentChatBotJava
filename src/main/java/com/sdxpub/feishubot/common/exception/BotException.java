package com.sdxpub.feishubot.common.exception;

import lombok.Getter;

@Getter
public class BotException extends RuntimeException {
    private final String code;
    private final String message;

    public BotException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BotException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public BotException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public BotException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BotException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}
