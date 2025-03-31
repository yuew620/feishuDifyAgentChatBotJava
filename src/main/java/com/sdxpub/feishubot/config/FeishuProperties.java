package com.sdxpub.feishubot.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {
    private String appId;
    private String appSecret;
    private String verificationToken;
    private String encryptKey;
    private boolean enableEncrypt = false;
    private String apiEndpoint = "https://open.feishu.cn/open-apis";
}
