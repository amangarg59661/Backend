package com.edss.notifications.application;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * PF-03: bounded executor for {@link NotificationDispatcher#deliverAsync}.
 * Bounded core / max / queue so a slow downstream (Twilio, Resend) cannot
 * starve the JVM. CallerRunsPolicy on saturation applies backpressure to the
 * outbox relay thread rather than dropping notifications silently.
 */
@Configuration
class NotificationsExecutorConfig {

    @Bean(name = "notificationsExecutor")
    Executor notificationsExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setThreadNamePrefix("notify-");
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(16);
        exec.setQueueCapacity(500);
        exec.setKeepAliveSeconds(60);
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.initialize();
        return exec;
    }
}
