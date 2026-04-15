package com.epam.sonar.core.logging;

import com.epam.sonar.data.model.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════════
 * 🔴 LOGGING ISSUES DEMO
 *
 * Covers Sonar logging rules:
 *   S2629  — Expensive logger arguments evaluated before guard check
 *   S3457  — Printf-style format strings used with SLF4J
 *   S5145  — Sensitive data logged (credentials, tokens, PII)
 *   S4792  — Logger configuration should not be alterable
 *   S1312  — Loggers should be private static final
 *   S2139  — Exceptions should be logged or rethrown, not both
 *   S106   — Standard output (System.out/err) used instead of logger
 *
 * Also demonstrates CORRECT patterns for comparison.
 * ══════════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
public class LoggingIssuesDemo {

    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #1: Non-private, non-static, non-final logger
    // Sonar rule: S1312 — Loggers should be private static final
    // ──────────────────────────────────────────────────────────────

    // ❌ BAD: public, instance-level, mutable logger
    public org.slf4j.Logger badLogger =
            org.slf4j.LoggerFactory.getLogger(LoggingIssuesDemo.class);

    // ✅ GOOD: @Slf4j annotation generates: private static final Logger log = ...


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #2: Expensive string operation in log arg
    // not guarded by level check
    // Sonar rule: S2629 — Expensive logger arguments
    // ──────────────────────────────────────────────────────────────
    public void processUsers(java.util.List<User> users) {
        // ❌ BAD: buildReport() runs even when DEBUG is disabled
        // If log level is INFO, this computation is wasted every call
        log.debug("Processing: " + buildExpensiveReport(users)); // ❌ string concat in arg

        // ❌ BAD: toString() on large collection always evaluated
        log.debug("All users: {}", users.toString()); // ❌ .toString() forced eagerly

        // ✅ GOOD: Guard check prevents expensive evaluation
        if (log.isDebugEnabled()) {
            log.debug("Processing: {}", buildExpensiveReport(users));
        }

        // ✅ GOOD: SLF4J defers toString() evaluation — only called if DEBUG enabled
        log.debug("User count: {}", users.size());
    }

    private String buildExpensiveReport(java.util.List<User> users) {
        // Simulate expensive operation (DB calls, serialization, etc.)
        StringBuilder sb = new StringBuilder();
        users.forEach(u -> sb.append(u.getUsername()).append(","));
        return sb.toString();
    }


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #3: Printf-style format used with SLF4J
    // Sonar rule: S3457 — Printf-style format strings
    // ──────────────────────────────────────────────────────────────
    public void loginAttempt(String username, int attempts) {
        // ❌ BAD: %s/%d are printf-style — SLF4J uses {} placeholders
        // This logs a literal "%s logged in, attempts: %d" string
        log.info(String.format("User %s logged in, attempts: %d", username, attempts)); // ❌ String.format unnecessary

        // ❌ BAD: same anti-pattern with concatenation
        log.warn("User " + username + " has " + attempts + " failed attempts"); // ❌ concat

        // ✅ GOOD: SLF4J parameterized logging — lazy, efficient, no format issues
        log.info("User {} logged in, attempts: {}", username, attempts);
        log.warn("User {} has {} failed attempts", username, attempts);
    }


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #4: Sensitive data logged
    // Sonar rule: S5145 — Sensitive data in logs
    // ──────────────────────────────────────────────────────────────
    public void authenticateUser(String username, String password, String token) {
        // ❌ BAD: password logged in plain text — appears in log files, SIEM, Splunk
        log.info("Auth attempt — user: {}, password: {}", username, password);

        // ❌ BAD: JWT/session token logged — can be replayed by log reader
        log.debug("User token: {}", token);

        // ❌ BAD: full credit card number logged
        String cardNumber = "4111111111111111";
        log.info("Processing payment for card: {}", cardNumber);

        // ✅ GOOD: mask sensitive values before logging
        log.info("Auth attempt — user: {}, password: [REDACTED]", username);
        log.debug("User token: {}...{}", token.substring(0, 6), token.substring(token.length() - 4));
        log.info("Processing payment for card: ****{}", cardNumber.substring(cardNumber.length() - 4));
    }


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #5: Exception logged AND rethrown
    // Sonar rule: S2139 — Exceptions should be either logged or rethrown
    // Causes duplicate log entries up the call stack
    // ──────────────────────────────────────────────────────────────
    public User findUser(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("ID cannot be null");
            return null; // placeholder
        } catch (IllegalArgumentException e) {
            // ❌ BAD: exception logged here AND rethrown — will be logged again by caller
            log.error("Failed to find user: {}", e.getMessage());
            throw e; // ❌ double-logging — stack trace appears twice in logs
        }
    }

    // ✅ GOOD: Log OR rethrow — not both
    public User findUserClean(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("ID cannot be null");
            return null;
        } catch (IllegalArgumentException e) {
            // Option A: log and handle
            log.error("Failed to find user with id {}: {}", id, e.getMessage());
            return null;
            // Option B: rethrow with context (caller logs it)
            // throw new UserNotFoundException("User not found: " + id, e);
        }
    }


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #6: System.out/err used instead of logger
    // Sonar rule: S106 — Standard outputs should not be used directly
    // ──────────────────────────────────────────────────────────────
    public void processPayment(double amount) {
        // ❌ BAD: System.out bypasses log level, format, rotation, structured output
        System.out.println("Processing payment: " + amount);          // ❌
        System.err.println("WARNING: Large payment amount: " + amount); // ❌
        // Output goes to stdout/stderr — not captured by log framework

        // ✅ GOOD: use SLF4J logger
        log.info("Processing payment: {}", amount);
        if (amount > 10000) {
            log.warn("Large payment amount detected: {}", amount);
        }
    }


    // ──────────────────────────────────────────────────────────────
    // 🔴 LOGGING ISSUE #7: Exception message logged without stack trace
    // Losing the root cause — hard to debug in production
    // ──────────────────────────────────────────────────────────────
    public void saveOrder(Object order) {
        try {
            // simulate save
            throw new RuntimeException("DB connection timeout");
        } catch (Exception e) {
            // ❌ BAD: only logs message string — stack trace lost forever
            log.error("Save failed: {}", e.getMessage()); // ❌ no exception passed

            // ✅ GOOD: pass exception as last arg — Logback logs full stack trace
            // log.error("Save failed for order: {}", order, e);
        }
    }


    // ──────────────────────────────────────────────────────────────
    // ✅ CORRECT PATTERN: MDC for request tracing
    // MDC (Mapped Diagnostic Context) adds correlation IDs to every
    // log line within a request — essential for distributed tracing
    // ──────────────────────────────────────────────────────────────
    public void processWithMdc(String userId, String action) {
        // Set request context — appears in every log line until cleared
        MDC.put("userId", userId);
        MDC.put("requestId", UUID.randomUUID().toString());
        MDC.put("action", action);

        try {
            log.info("Starting action: {}", action);       // → includes userId, requestId in JSON output
            log.debug("Validating permissions for action"); // → same MDC context
            log.info("Action completed successfully");
        } finally {
            MDC.clear(); // ✅ Always clear MDC — thread pool reuse will carry stale context
        }
    }
}
