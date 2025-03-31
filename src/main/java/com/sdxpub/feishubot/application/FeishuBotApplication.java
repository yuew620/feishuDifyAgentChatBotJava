package com.sdxpub.feishubot.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sdxpub.feishubot")
@EnableCaching
@EnableScheduling
public class FeishuBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeishuBotApplication.class, args);
    }
}
