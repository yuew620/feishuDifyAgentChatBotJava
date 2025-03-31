package com.sdxpub.feishubot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {
    private String appId;
    private String appSecret;
    private String verificationToken;
    private String apiEndpoint = "https://open.feishu.cn/open-apis";
    private String chatId;
}
