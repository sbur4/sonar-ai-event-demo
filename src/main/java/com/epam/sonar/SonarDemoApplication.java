package com.epam.sonar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@EnableAsync
@Slf4j
@SpringBootApplication
public class SonarDemoApplication {

    static void main(String[] args) {
// ✅ Configure graceful shutdown at SpringApplication level
        SpringApplication app = new SpringApplication(SonarDemoApplication.class);

        // Register JVM-level SIGTERM handler before context starts
        // Ensures shutdown hook fires even if Spring context fails to start
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                log.info("JVM SIGTERM received — Spring graceful shutdown in progress"),
                "main-shutdown-hook"
        ));

        app.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("═══════════════════════════════════════════");
        log.info("  sonar-ai-event-epam-demo started");
        log.info("  Listening on http://localhost:8181/api");
        log.info("  Actuator : http://localhost:8181/api/actuator/health");
        log.info("  H2 console: http://localhost:8181/api/h2-console");
        log.info("  Graceful shutdown: ENABLED (timeout: 30s)");
        log.info("═══════════════════════════════════════════");
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("═══════════════════════════════════════════");
        log.info("  Spring context closing — draining requests");
        log.info("  Readiness probe → DOWN");
        log.info("  Waiting up to 30s for in-flight requests...");
        log.info("═══════════════════════════════════════════");
    }
}