package com.epam.sonar.core.service;

import com.epam.sonar.data.model.Order;
import com.epam.sonar.data.model.User;
import com.epam.sonar.data.repository.OrderRepository;
import com.epam.sonar.data.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: MEMORY LEAKS
    // ══════════════════════════════════════════════════════

    // 🔴 Memory leak: Static cache that grows unboundedly — never evicted
    private static final Map<String, List<Object>> REPORT_CACHE = new HashMap<>(); // ❌ Unbounded static map

    // 🔴 Memory leak: Static list accumulates data forever in singleton
    private static final List<String> auditLog = new ArrayList<>(); // ❌ Never cleared

    public ReportService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    // 🔴 S2175 - Loading entire table into memory for reporting
    // For large datasets this causes OutOfMemoryError
    public Map<String, BigDecimal> getRevenueByUser() {
        // ❌ Loads ALL orders into heap — no pagination, no streaming
        List<Order> allOrders = orderRepository.findAll();
        Map<String, BigDecimal> revenue = new HashMap<>();

        for (Order order : allOrders) {
            String key = order.getUser() != null ? order.getUser().getUsername() : "unknown";
            revenue.merge(key,
                    order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())),
                    BigDecimal::add);
        }

        // ❌ Unbounded cache — grows with every unique report key, never evicted
        REPORT_CACHE.put("revenue_" + System.currentTimeMillis(), new ArrayList<>(allOrders));
        auditLog.add("Revenue report generated at " + new Date()); // ❌ Memory accumulates forever

        return revenue;
    }

    // 🔴 S4276 - Inefficient stream usage — collecting then streaming again
    public List<String> getTopUsernames(int limit) {
        // ❌ Loads all users, collects to list, then streams again — wasteful
        List<User> allUsers = userRepository.findAll()
                .stream()
                .collect(Collectors.toList()); // ❌ Unnecessary intermediate collection

        return allUsers.stream()
                .filter(User::isActive)
                .map(User::getUsername)
                .sorted()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 🔴 S2250 - Collection.contains() on a List — O(n) for every check
    // Should use a Set for membership tests
    public List<User> filterPremiumUsers(List<User> users) {
        // ❌ premiumIds is a List — .contains() is O(n), making whole method O(n²)
        List<Long> premiumIds = orderRepository.findAll()
                .stream()
                .filter(o -> o.getPrice().compareTo(BigDecimal.valueOf(100)) > 0)
                .map(o -> o.getUser() != null ? o.getUser().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()); // ❌ Should be a Set

        return users.stream()
                .filter(u -> premiumIds.contains(u.getId())) // ❌ O(n) lookup in a List
                .toList();
    }

    // 🔴 Code smell: Returning null instead of Optional or empty collection
    public List<Order> getOrdersForUser(Long userId) {
        try {
            return orderRepository.findByUserId(userId);
        } catch (Exception e) {
            return null; // ❌ Callers must null-check — should return empty list or Optional
        }
    }

    // 🔴 S1481 - Unused local variable (dead store)
    public BigDecimal calculateDiscount(Order order) {
        BigDecimal originalPrice = order.getPrice(); // used
        BigDecimal taxRate = BigDecimal.valueOf(0.20); // ❌ assigned but never used
        BigDecimal discount = BigDecimal.ZERO;

        if (order.getQuantity() > 10) {
            discount = originalPrice.multiply(BigDecimal.valueOf(0.10));
        }
        return discount;
    }
}
