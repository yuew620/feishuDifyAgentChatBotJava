package com.sdxpub.feishubot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {
    private String apiEndpoint;
    private String apiKey;
}
