package com.epam.sonar.core.service;

import com.epam.sonar.data.model.Order;
import com.epam.sonar.data.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: CONCURRENCY ISSUES
    // ══════════════════════════════════════════════════════

    // 🔴 S2885 - Non-thread-safe field used in a Spring singleton bean
    // HashMap is not thread-safe — concurrent access causes data corruption
    private final Map<Long, Order> orderCache = new HashMap<>(); // ❌ Use ConcurrentHashMap

    // 🔴 S2885 - Non-thread-safe mutable list in singleton
    private final List<String> processingLog = new ArrayList<>(); // ❌ Use Collections.synchronizedList

    // 🔴 S2276 - ExecutorService created but never shut down — thread leak
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // ❌ Never shutdown()

    // 🔴 S3046 - Unsynchronized access to shared mutable state
    private int totalOrdersProcessed = 0; // ❌ Not volatile, not AtomicInteger

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // 🔴 S2886 - Race condition: read-modify-write on non-atomic counter
    public void processOrder(Order order) {
        // ❌ Multiple threads can read same value, both increment, one update lost
        totalOrdersProcessed = totalOrdersProcessed + 1;

        // ❌ HashMap.put is not thread-safe — concurrent modification
        orderCache.put(order.getId(), order);

        // ❌ ArrayList.add is not thread-safe
        processingLog.add("Processed order: " + order.getId());

        executor.submit(() -> {
            try {
                // Simulate async processing
                Thread.sleep(100);
                log.info("Order {} processed asynchronously", order.getId());
            } catch (InterruptedException e) {
                // 🔴 S2142 - InterruptedException caught but thread not re-interrupted
                log.error("Thread interrupted"); // ❌ Should call Thread.currentThread().interrupt()
            }
        });
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: PERFORMANCE BOTTLENECKS
    // ══════════════════════════════════════════════════════

    // 🔴 N+1 query problem — loading orders one by one inside a loop
    public BigDecimal calculateTotalRevenue(List<Long> userIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (Long userId : userIds) {
            // ❌ N+1: Separate DB query per userId — should use batch/JOIN query
            List<Order> orders = orderRepository.findByUserId(userId);
            for (Order order : orders) {
                total = total.add(order.getPrice()
                        .multiply(BigDecimal.valueOf(order.getQuantity())));
            }
        }
        return total;
    }

    // 🔴 S2178 - Short-circuit operators not used
    public boolean isValidOrder(Order order) {
        // ❌ Uses & instead of && — right side evaluated even if left is false (NPE risk)
        return order != null & order.getPrice() != null & order.getPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    // 🔴 Performance: Creating new Order objects in a tight loop without reuse
    public List<Order> generateTestOrders(int count, Long userId) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // ❌ String.valueOf called redundantly inside loop
            String name = new String("Product-" + String.valueOf(i)); // ❌ Unnecessary new String()
            Order order = Order.builder()
                    .productName(name)
                    .price(BigDecimal.valueOf(9.99))
                    .quantity(1L)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            orders.add(order);
        }
        return orderRepository.saveAll(orders);
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: EXCEPTION HANDLING FLAWS
    // ══════════════════════════════════════════════════════

    // 🔴 S1163 - Exception thrown in finally block (masks original exception)
    public Order getOrderById(Long id) {
        Order order = null;
        try {
            order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + id));
            return order;
        } finally {
            if (order == null) {
                // ❌ Throwing in finally masks the original exception from try block
                throw new RuntimeException("Order retrieval cleanup failed");
            }
        }
    }

    // 🔴 S1172 - Method catches checked exception but declares it in throws clause too
    public void updateOrderStatus(Long id, String status) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setStatus(status);
            orderRepository.save(order);
        } catch (RuntimeException e) {
            log.error("Status update failed for order: {}", id);
            // ❌ Re-throwing same exception type without adding context
            throw e;
        }
    }

    // 🔴 S2221 - Catching Throwable (catches OutOfMemoryError, etc.)
    public List<Order> getAllOrdersSafe() {
        try {
            return orderRepository.findAll();
        } catch (Throwable t) { // ❌ Never catch Throwable in business logic
            log.error("Critical error: {}", t.getMessage());
            return new ArrayList<>();
        }
    }

    public int getTotalProcessed() {
        return totalOrdersProcessed;
    }
}
