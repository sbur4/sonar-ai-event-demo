package com.epam.sonar.data.scheduler;

import com.epam.sonar.data.repository.OrderRepository;
import com.epam.sonar.data.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class CleanupScheduler {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    // 🔴 Concurrency: Lock acquired but potentially never released on exception
    private final Lock cleanupLock = new ReentrantLock();

    public CleanupScheduler(UserRepository userRepository,
                            OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    // 🔴 S2222 - Lock acquired but not released in all paths (missing try-finally)
    @Scheduled(fixedDelay = 60000)
    public void cleanupInactiveUsers() {
        cleanupLock.lock(); // ❌ Lock not in try-finally — deadlock if exception thrown
        log.info("Starting cleanup of inactive users...");

        try {
            long count = userRepository.findAll()
                    .stream()
                    .filter(u -> !u.isActive())
                    .count();
            log.info("Found {} inactive users", count);
            // Simulate cleanup work
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage());
            // ❌ Lock never released if exception occurs before unlock()
        }

        cleanupLock.unlock(); // ❌ Must be in finally block
    }

    // 🔴 S2095 - Resource leak in scheduled task
    @Scheduled(cron = "0 0 1 * * ?")
    public void exportDailyReport() {
        String reportPath = "/tmp/daily-report.csv";
        try {
            // ❌ FileWriter not in try-with-resources — leaks on exception
            FileWriter writer = new FileWriter(reportPath, true);
            writer.write("Daily Report — " + java.time.LocalDate.now() + "\n");

            orderRepository.findAll().forEach(order ->
                    // ❌ IOException from writer.write() not handled inside lambda
                    log.info("Order: {}", order.getId())
            );

            writer.close(); // ❌ Only closed on happy path
            log.info("Daily report exported to {}", reportPath);
        } catch (IOException e) {
            log.error("Report export failed: {}", e.getMessage());
        }
    }

    // 🔴 S2142 - InterruptedException swallowed — thread interrupt state lost
    @Scheduled(fixedDelay = 30000)
    public void processQueue() {
        try {
            Thread.sleep(500); // Simulate processing delay
            log.info("Queue processed");
        } catch (InterruptedException e) {
            // ❌ Interrupt status cleared — calling code can't detect thread was interrupted
            log.error("Queue processing interrupted");
            // Should call: Thread.currentThread().interrupt();
        }
    }
}
