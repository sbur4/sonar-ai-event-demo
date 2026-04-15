package com.epam.sonar.web.config;

import com.epam.sonar.core.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * ══════════════════════════════════════════════════════════════════
 * GRACEFUL SHUTDOWN CONFIGURATION
 *
 * Coordinates a clean, ordered shutdown sequence:
 *
 *  1. Readiness probe → DOWN  (Kubernetes/load balancer stops routing)
 *  2. Wait for drain-delay    (in-flight requests finish, default 30s)
 *  3. Jetty connector paused  (no new connections accepted)
 *  4. Task executor drained   (async tasks allowed to complete)
 *  5. Scheduler shutdown      (no new scheduled tasks started)
 *  6. @PreDestroy hooks run   (release resources, flush buffers)
 *  7. Spring context closed   (beans destroyed)
 *  8. JVM exits
 *
 * Spring Boot 3 handles steps 1–3 automatically when:
 *   server.shutdown=graceful
 *   spring.lifecycle.timeout-per-shutdown-phase=30s
 *
 * This class handles steps 4–6 explicitly.
 * ══════════════════════════════════════════════════════════════════
 */
@Configuration
@Slf4j
public class GracefulShutdownConfig {

    // ──────────────────────────────────────────────────────────────
    // Async task executor — used by @Async methods across the app
    // Configured with graceful termination so in-flight async tasks
    // are allowed to finish before the JVM exits
    // ──────────────────────────────────────────────────────────────
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setThreadGroupName("sonar-demo-async");

        // ✅ Graceful shutdown settings
        executor.setWaitForTasksToCompleteOnShutdown(true);  // drain queue before exit
        executor.setAwaitTerminationSeconds(30);              // max wait time
        executor.setAwaitTerminationMillis(30_000);

        executor.initialize();
        return executor;
    }

    // ──────────────────────────────────────────────────────────────
    // Task scheduler — used by @Scheduled methods
    // Prevents new scheduled tasks from starting during shutdown
    // ──────────────────────────────────────────────────────────────
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setErrorHandler(t ->
                log.error("Scheduled task error: {}", t.getMessage(), t));

        // ✅ Graceful shutdown settings
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);

        return scheduler;
    }

    // ──────────────────────────────────────────────────────────────
    // Jetty customizer — configures stop timeout at server level
    // Gives Jetty time to finish serving active requests before
    // forcibly closing connections
    // ──────────────────────────────────────────────────────────────
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyGracefulShutdown() {
        return factory -> {
            factory.addServerCustomizers(server -> {
                // Max time Jetty waits for active requests to complete
                server.setStopTimeout(30_000);
            });
        };
    }

    // ──────────────────────────────────────────────────────────────
    // Application event listener — acts on context close signal
    // Logs shutdown sequence and performs pre-close preparation
    // ──────────────────────────────────────────────────────────────
    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownListener(
            OrderService orderService) {
        return event -> {
            log.info("═══════════════════════════════════════════");
            log.info("Shutdown signal received — beginning graceful drain");
            log.info("In-flight orders processed: {}", orderService.getTotalProcessed());
            log.info("Waiting for active requests and async tasks to complete...");
            log.info("═══════════════════════════════════════════");
        };
    }
}
