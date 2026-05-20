package com.project.Blog_Management_System.Config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "emailTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("email-async-");

        // graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // overload handling
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy());

        // Propagate SLF4J MDC to async threads so email/scheduler logs
        // include the same requestId and userId as the originating HTTP thread.
        executor.setTaskDecorator(
                new MdcPropagatingTaskDecorator());

        executor.initialize();

        return executor;
    }

    /**
     * Copies the SLF4J MDC map from the calling thread into the async thread.
     * Without this, all @Async method logs show null requestId and userId
     * because ThreadLocal MDC doesn't cross thread boundaries.
     */
    static class MdcPropagatingTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();

            return () -> {
                try {
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}