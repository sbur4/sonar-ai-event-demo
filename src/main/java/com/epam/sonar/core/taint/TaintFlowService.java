package com.epam.sonar.core.taint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════
 * 🔴 CROSS-CLASS TAINT FLOW SERVICE
 *
 * Sonar Advanced SAST tracks taint as it flows from controllers
 * into services and then into dangerous sinks.
 *
 * Standard SAST tools often miss these — they only analyze single
 * methods. Sonar Advanced SAST performs inter-procedural analysis.
 * ══════════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
public class TaintFlowService {

    private final JdbcTemplate jdbcTemplate;

    public TaintFlowService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT SINK: Path traversal — called from TaintTrackingController
    // Sonar traces: Controller.downloadFile() → this.readFile() → Files.readAllBytes()
    // Sonar rule: S2083
    // ──────────────────────────────────────────────────────────────
    public String readFile(String filename) {
        try {
            // ❌ SINK: tainted filename (from HTTP header) reaches filesystem
            // Sonar tracks the taint origin back to the HTTP header source
            byte[] bytes = Files.readAllBytes(Paths.get("/app/files/" + filename));
            return new String(bytes);
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT FLOW: Taint stored in object, retrieved and used in sink
    // Demonstrates taint surviving object assignment and retrieval
    // Sonar rule: S2077
    // ──────────────────────────────────────────────────────────────
    public List<Map<String, Object>> findUsersByFilter(UserFilter filter) {
        // ❌ Taint from filter.getField() and filter.getValue() flows into SQL
        // Even though taint is wrapped in an object, Sonar tracks through it
        String sql = "SELECT * FROM users WHERE "
                + filter.getField()           // ❌ tainted field name
                + " = '"
                + filter.getValue()           // ❌ tainted field value
                + "'";
        return jdbcTemplate.queryForList(sql);
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT FLOW: Taint through string transformation chain
    // Each step transforms but does NOT sanitize — taint persists
    // Sonar rule: S2077
    // ──────────────────────────────────────────────────────────────
    public List<Map<String, Object>> searchWithTransformation(String rawInput) {
        String step1 = rawInput.trim();              // taint persists after trim()
        String step2 = step1.toLowerCase();          // taint persists after toLowerCase()
        String step3 = "%" + step2 + "%";            // taint persists in concatenation
        // ❌ SINK: still tainted despite transformations
        return jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE product_name LIKE '" + step3 + "'"
        );
    }

    /**
     * Simple value object — Sonar tracks taint through field assignment/retrieval
     */
    public record UserFilter(String field, String value) {
        public String getField() {
            return field;
        }

        public String getValue() {
            return value;
        }
    }
}
