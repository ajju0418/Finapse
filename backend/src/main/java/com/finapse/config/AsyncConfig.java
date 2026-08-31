package com.finapse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor for statement imports. Parsing a large PDF can take several seconds,
 * which is too long to hold an HTTP request open, so uploads are processed off
 * the request thread and the client polls the statement's import status.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "statementImportExecutor")
    public Executor statementImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("stmt-import-");
        // Back-pressure: when the queue is full the caller runs the task itself
        // rather than silently dropping an upload the user already paid for.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
