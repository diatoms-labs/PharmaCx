package com.pharmaCx.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables Spring @Async support for fire-and-forget audit event persistence
 * and background AI document indexing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated thread pool for AI background tasks (document indexing).
     * Kept small (2 threads) so indexing never starves document-serving threads.
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-index-");
        executor.initialize();
        return executor;
    }
}
