package com.alemandan.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for sending emails asynchronously after transaction commit.
 * This prevents SMTP timeouts from blocking HTTP responses.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool executor for async email operations.
     * Core pool: 2 threads, Max: 5 threads, Queue: 50 tasks
     */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}
