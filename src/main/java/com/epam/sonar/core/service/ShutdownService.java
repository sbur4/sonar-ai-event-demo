package com.epam.sonar.core.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ══════════════════════════════════════════════════════════════════
 * SHUTDOWN HOOK SERVICE
 *
 * Demonstrates proper resource cleanup on application shutdown:
 *
 * - @PreDestroy   — Spring calls this before destroying the bean
 * - ExecutorService.shutdown() + awaitTermination() — drain pattern
 * - JVM shutdown hook via Runtime.addShutdownHook() — last resort
 *
 * Also shows common shutdown ANTI-PATTERNS that Sonar detects:
 * - S2276  — ExecutorService never shut down (thread leak)
 * - S2142  — InterruptedException not re-interrupted on shutdown
 * ══════════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
public class ShutdownService {

    // ✅ CORRECT: executor that will be properly drained on shutdown
    private final ExecutorService managedExecutor =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "managed-worker");
                t.setDaemon(false); // non-daemon — JVM waits for it to finish
                return t;
            });

    // 🔴 SONAR ISSUE: S2276 — ExecutorService created but never shut down
    // Thread leak — 4 threads live until JVM force-killed
    private final ExecutorService leakedExecutor =
            Executors.newFixedThreadPool(4); // ❌ no shutdown() call anywhere

    public ShutdownService() {
        // Register JVM shutdown hook as last-resort safety net
        // This fires even on SIGTERM/SIGKILL when Spring context teardown fails
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("JVM shutdown hook triggered — forcing executor drain");
            forceShutdownExecutor(managedExecutor, "managed-executor", 5);
        }, "shutdown-hook"));

        log.info("ShutdownService initialized — JVM shutdown hook registered");
    }

    // ──────────────────────────────────────────────────────────────
    // ✅ CORRECT: @PreDestroy — Spring calls this during context close,
    // before the bean is destroyed. Ordered before JVM shutdown hook.
    // ──────────────────────────────────────────────────────────────
    @PreDestroy
    public void onShutdown() {
        log.info("@PreDestroy triggered — starting resource cleanup");

        // Step 1: stop accepting new tasks
        managedExecutor.shutdown();
        log.info("Executor shutdown initiated — no new tasks accepted");

        // Step 2: wait for in-flight tasks to complete
        try {
            if (!managedExecutor.awaitTermination(25, TimeUnit.SECONDS)) {
                // Step 3: tasks didn't finish in time — force stop
                log.warn("Executor did not terminate in 25s — forcing shutdown");
                managedExecutor.shutdownNow();

                // Step 4: wait a bit more for forced shutdown
                if (!managedExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate after shutdownNow()");
                }
            } else {
                log.info("Executor drained cleanly — all tasks completed");
            }
        } catch (InterruptedException e) {
            // ✅ CORRECT: re-interrupt the thread — do NOT swallow this
            log.warn("Shutdown interrupted — forcing executor shutdown");
            managedExecutor.shutdownNow();
            Thread.currentThread().interrupt(); // ✅ restore interrupt status
        }

        log.info("ShutdownService cleanup complete");
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 ANTI-PATTERN: InterruptedException swallowed during shutdown
    // Sonar rule: S2142
    // ──────────────────────────────────────────────────────────────
    public void badShutdownPattern(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ❌ BAD: interrupt status cleared — calling thread never knows
            log.error("Interrupted during shutdown"); // ❌ no re-interrupt
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Reusable drain utility for multiple executor instances
    // ──────────────────────────────────────────────────────────────
    private void forceShutdownExecutor(ExecutorService executor,
                                       String name,
                                       int timeoutSeconds) {
        if (executor.isTerminated()) return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Force-stopping executor '{}' after {}s timeout", name, timeoutSeconds);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // ✅ always re-interrupt
        }
    }

    public void submitTask(Runnable task) {
        if (!managedExecutor.isShutdown()) {
            managedExecutor.submit(task);
        } else {
            log.warn("Task rejected — executor is shutting down");
        }
    }
}
