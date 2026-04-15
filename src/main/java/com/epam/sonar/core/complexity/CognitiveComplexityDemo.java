package com.epam.sonar.core.complexity;

import com.epam.sonar.data.model.Order;
import com.epam.sonar.data.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════
 * 🔴 COGNITIVE COMPLEXITY DEMO
 *
 * Sonar rule: S3776 — Cognitive Complexity should not be too high
 * Default threshold: 15
 *
 * Cognitive Complexity differs from Cyclomatic Complexity:
 * - It penalizes NESTING more than branching
 * - It penalizes BREAKS in linear flow (loops, recursion, jumps)
 * - It penalizes structural complexity humans find hard to read
 *
 * Each method below has its estimated Cognitive Complexity score.
 * ══════════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
public class CognitiveComplexityDemo {

    // ──────────────────────────────────────────────────────────────
    // 🔴 COMPLEXITY #1 — Estimated score: ~35 (threshold: 15)
    // Deep nesting + multiple conditions + loops within conditions
    // ──────────────────────────────────────────────────────────────
    public String processOrderWorkflow(Order order, User user, boolean isPriority) {
        // +1 if
        if (order != null) {
            // +2 if (nested)
            if (user != null && user.isActive()) {
                // +3 if (nested)
                if (order.getPrice() != null) {
                    // +4 if (nested)
                    if (order.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        // +5 if (nested)
                        if (isPriority) {
                            // +6 if (nested)
                            if (user.getRole() != null) {
                                // +7 switch (nested)
                                switch (user.getRole()) {
                                    case "ADMIN":
                                        // +8 for (nested)
                                        for (int i = 0; i < 3; i++) {
                                            // +9 if (nested)
                                            if (order.getStatus() != null) {
                                                log.info("Admin priority retry {}", i);
                                            }
                                        }
                                        return "ADMIN_PRIORITY";
                                    case "MANAGER":
                                        return "MANAGER_PRIORITY";
                                    default:
                                        // +8 if (nested)
                                        if (order.getQuantity() > 10) {
                                            return "BULK_PRIORITY";
                                        }
                                        return "STANDARD_PRIORITY";
                                }
                            } else {
                                return "NO_ROLE_PRIORITY";
                            }
                        } else {
                            // +6 if (nested)
                            if (order.getQuantity() <= 0) {
                                return "INVALID_QUANTITY";
                            }
                            // +6 if (nested)
                            if (order.getProductName() == null || order.getProductName().isEmpty()) {
                                return "NO_PRODUCT";
                            }
                            return "STANDARD";
                        }
                    } else {
                        return "INVALID_PRICE";
                    }
                } else {
                    return "NULL_PRICE";
                }
            } else {
                // +2 if (nested)
                if (user == null) {
                    return "NO_USER";
                }
                return "INACTIVE_USER";
            }
        }
        return "NULL_ORDER";
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 COMPLEXITY #2 — Estimated score: ~28
    // Mixed loops, conditions, and early returns with complex logic
    // ──────────────────────────────────────────────────────────────
    public BigDecimal calculateComplexDiscount(List<Order> orders, User user,
                                               Map<String, BigDecimal> promotions,
                                               boolean isBlackFriday, boolean isMember) {
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // +1 if
        if (orders == null || orders.isEmpty()) {
            return totalDiscount;
        }

        // +1 for
        for (Order order : orders) {
            BigDecimal orderDiscount = BigDecimal.ZERO;

            // +2 if (nested)
            if (isBlackFriday) {
                // +3 if (nested)
                if (order.getPrice().compareTo(BigDecimal.valueOf(100)) > 0) {
                    orderDiscount = orderDiscount.add(order.getPrice().multiply(BigDecimal.valueOf(0.30)));
                    // +4 if (nested)
                    if (isMember) {
                        orderDiscount = orderDiscount.add(order.getPrice().multiply(BigDecimal.valueOf(0.05)));
                    }
                } else {
                    orderDiscount = orderDiscount.add(order.getPrice().multiply(BigDecimal.valueOf(0.15)));
                }
            } else if (isMember) { // +2 else-if
                // +3 if (nested)
                if (order.getQuantity() >= 5) {
                    orderDiscount = order.getPrice().multiply(BigDecimal.valueOf(0.10));
                    // +4 if (nested)
                    if (order.getQuantity() >= 10) {
                        orderDiscount = order.getPrice().multiply(BigDecimal.valueOf(0.20));
                    }
                }
            }

            // +2 if (nested in for)
            if (promotions != null && !promotions.isEmpty()) {
                // +3 for (nested)
                for (Map.Entry<String, BigDecimal> promo : promotions.entrySet()) {
                    // +4 if (nested)
                    if (order.getProductName() != null &&
                            order.getProductName().contains(promo.getKey())) {
                        // +5 if (nested)
                        if (promo.getValue().compareTo(orderDiscount) > 0) {
                            orderDiscount = promo.getValue();
                        }
                    }
                }
            }

            totalDiscount = totalDiscount.add(orderDiscount);
        }

        // +1 if
        if (totalDiscount.compareTo(BigDecimal.valueOf(500)) > 0) {
            // +2 if (nested)
            if (!isMember) {
                totalDiscount = BigDecimal.valueOf(500);
            }
        }

        return totalDiscount;
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 COMPLEXITY #3 — Estimated score: ~22
    // Recursive method with multiple branches — recursion adds +1
    // ──────────────────────────────────────────────────────────────
    public List<String> buildPermissionTree(String role, int depth, List<String> visited) {
        List<String> permissions = new ArrayList<>();

        // +1 if
        if (depth > 10 || role == null) {
            return permissions;
        }

        // +1 if
        if (visited == null) {
            visited = new ArrayList<>();
        }

        // +1 if
        if (visited.contains(role)) {
            return permissions; // prevent circular references
        }

        visited.add(role);

        // +1 switch
        switch (role) {
            case "SUPER_ADMIN":
                permissions.add("READ");
                permissions.add("WRITE");
                permissions.add("DELETE");
                permissions.add("ADMIN");
                // +2 recursive call (nested + recursion penalty)
                permissions.addAll(buildPermissionTree("ADMIN", depth + 1, visited));
                break;
            case "ADMIN":
                permissions.add("READ");
                permissions.add("WRITE");
                permissions.add("DELETE");
                // +2 if (nested)
                if (depth < 5) {
                    // +3 recursive call (nested)
                    permissions.addAll(buildPermissionTree("MANAGER", depth + 1, visited));
                }
                break;
            case "MANAGER":
                permissions.add("READ");
                permissions.add("WRITE");
                // +2 if (nested)
                if (depth < 3) {
                    for (String subRole : List.of("ANALYST", "VIEWER")) { // +3 for (nested)
                        // +4 recursive (nested)
                        permissions.addAll(buildPermissionTree(subRole, depth + 1, visited));
                    }
                }
                break;
            case "ANALYST":
                permissions.add("READ");
                permissions.add("EXPORT");
                break;
            default:
                permissions.add("READ");
        }

        return permissions;
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 COMPLEXITY #4 — Estimated score: ~20
    // Long method with validation chain + many conditions
    // ──────────────────────────────────────────────────────────────
    public ValidationResult validateAndEnrichOrder(Order order, User user) {
        // +1 if
        if (order == null) {
            return ValidationResult.fail("Order is null");
        }

        // +1 if
        if (user == null) {
            return ValidationResult.fail("User is null");
        }

        // +1 if
        if (!user.isActive()) {
            return ValidationResult.fail("User account is inactive");
        }

        // +1 if
        if (order.getProductName() == null || order.getProductName().isBlank()) {
            return ValidationResult.fail("Product name is required");
        }

        // +1 if
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.fail("Price must be positive");
        }

        // +1 if
        if (order.getQuantity() <= 0 || order.getQuantity() > 10000) {
            return ValidationResult.fail("Quantity must be between 1 and 10000");
        }

        // Enrichment logic
        // +1 if
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        // +1 if
        if (order.getStatus() == null) {
            // +2 if (nested)
            if (user.getRole() != null && user.getRole().equals("ADMIN")) {
                order.setStatus("AUTO_APPROVED");
            } else {
                // +2 else (nested)
                // +3 if (nested)
                if (order.getPrice().compareTo(BigDecimal.valueOf(1000)) > 0) {
                    order.setStatus("PENDING_REVIEW");
                } else {
                    order.setStatus("PENDING");
                }
            }
        }

        // +1 if
        if (order.getUser() == null) {
            order.setUser(user);
        }

        // +1 try (adds to complexity)
        try {
            BigDecimal total = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            // +2 if (nested)
            if (total.compareTo(BigDecimal.valueOf(50000)) > 0) {
                // +3 if (nested)
                if (!user.getRole().equals("ADMIN")) {
                    return ValidationResult.fail("Order total exceeds limit for non-admin users");
                }
            }
        } catch (NullPointerException e) { // +1 catch
            return ValidationResult.fail("Calculation error: " + e.getMessage());
        }

        return ValidationResult.success(order);
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 COMPLEXITY #5 — Estimated score: ~18
    // Multi-condition boolean logic with nested loops
    // ──────────────────────────────────────────────────────────────
    public boolean hasConflictingOrders(List<Order> pendingOrders,
                                        List<Order> existingOrders,
                                        String conflictPolicy) {
        // +1 if
        if (pendingOrders == null || existingOrders == null) {
            return false;
        }

        // +1 for
        for (Order pending : pendingOrders) {
            // +2 for (nested)
            for (Order existing : existingOrders) {
                // +3 if (nested)
                if (pending.getProductName() != null
                        && existing.getProductName() != null
                        && pending.getProductName().equals(existing.getProductName())) {

                    // +4 switch (nested)
                    switch (conflictPolicy != null ? conflictPolicy : "STRICT") {
                        case "STRICT":
                            return true;
                        case "PRICE":
                            // +5 if (nested)
                            if (pending.getPrice() != null && existing.getPrice() != null) {
                                // +6 if (nested)
                                if (pending.getPrice().compareTo(existing.getPrice()) == 0) {
                                    return true;
                                }
                            }
                            break;
                        case "QUANTITY":
                            // +5 if (nested)
                            if (pending.getQuantity() == existing.getQuantity()) {
                                return true;
                            }
                            break;
                        default:
                            // +5 if (nested)
                            if (pending.getPrice() != null && existing.getPrice() != null
                                    && pending.getPrice().compareTo(existing.getPrice()) == 0
                                    && pending.getQuantity() == existing.getQuantity()) {
                                return true;
                            }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Simple result wrapper — clean, intentionally low complexity for contrast
     */
    public record ValidationResult(boolean valid, String message, Order order) {
        public static ValidationResult success(Order order) {
            return new ValidationResult(true, "OK", order);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message, null);
        }
    }
}
