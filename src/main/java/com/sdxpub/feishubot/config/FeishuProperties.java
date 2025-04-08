package com.sdxpub.feishubot.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {
    private String appId;
    private String appSecret;
    private String verificationToken;
    private String encryptKey;
    private boolean enableEncrypt = false;
    private String apiEndpoint = "https://open.feishu.cn/open-apis";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getEncryptKey() {
        return encryptKey;
    }

    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public boolean isEnableEncrypt() {
        return enableEncrypt;
    }

    public void setEnableEncrypt(boolean enableEncrypt) {
        this.enableEncrypt = enableEncrypt;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }
}
