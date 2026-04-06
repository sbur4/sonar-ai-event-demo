package com.epam.sonar.web.controller;

import com.epam.sonar.core.service.OrderService;
import com.epam.sonar.core.service.ReportService;
import com.epam.sonar.data.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ReportService reportService;

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // 🔴 S2259 - Return value from getOrdersForUser can be null — NPE on stream()
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        List<Order> orders = reportService.getOrdersForUser(userId);
        // ❌ orders can be null — .stream() will throw NPE
        long count = orders.stream().count();
        log.info("Found {} orders for user {}", count, userId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // ❌ No validation, no DTO — direct entity binding
        orderService.processOrder(order);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @RequestParam String status) {
        orderService.updateOrderStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, BigDecimal>> getRevenue() {
        return ResponseEntity.ok(reportService.getRevenueByUser());
    }

    @PostMapping("/generate-test")
    public ResponseEntity<List<Order>> generateTest(@RequestParam int count,
                                                    @RequestParam Long userId) {
        return ResponseEntity.ok(orderService.generateTestOrders(count, userId));
    }
}
