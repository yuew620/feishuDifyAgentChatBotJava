package com.sdxpub.feishubot.config;

import com.sdxpub.feishubot.common.constants.Constants;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Bean
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        executor.setRemoveOnCancelPolicy(true);
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("ScheduledThread-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        return executor;
    }
}
