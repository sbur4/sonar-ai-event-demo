package com.epam.sonar.web.controller;

import com.epam.sonar.core.taint.TaintFlowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════
 * 🔴 TAINT TRACKING DEMO
 *
 * Sonar Advanced SAST tracks "tainted" (user-controlled) data as it
 * flows across method calls, class boundaries, and data transformations.
 *
 * SOURCE  = where untrusted data enters (HTTP params, headers, body)
 * SINK    = where tainted data causes harm (SQL, file path, exec, log)
 * SANITIZER = where taint is neutralized (validation, encoding, escaping)
 *
 * All examples below are missing sanitizers — Sonar will trace the
 * full flow from SOURCE → SINK and flag each as a vulnerability.
 * ══════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/taint")
@Slf4j
public class TaintTrackingController {

    private final JdbcTemplate jdbcTemplate;
    private final TaintFlowService taintFlowService;

    public TaintTrackingController(JdbcTemplate jdbcTemplate,
                                   TaintFlowService taintFlowService) {
        this.jdbcTemplate = jdbcTemplate;
        this.taintFlowService = taintFlowService;
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #1: Direct SQL Injection (single-method taint flow)
    // SOURCE: @RequestParam "username"
    // SINK:   jdbcTemplate.queryForList(sql) — raw SQL execution
    // Sonar rule: S2077
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/users/search")
    public ResponseEntity<List<?>> searchUsers(@RequestParam String username) {
        // ❌ SOURCE: username is user-controlled
        // ❌ SINK: directly injected into SQL string — no parameterization
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #2: Cross-method taint flow
    // SOURCE: @RequestParam "query"
    // FLOW:   buildQuery() → executeQuery() → SINK
    // Sonar Advanced SAST traces across method boundaries
    // Sonar rule: S2077
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/orders/search")
    public ResponseEntity<List<?>> searchOrders(@RequestParam String query) {
        // ❌ SOURCE: query enters here
        String builtQuery = buildQuery(query);       // taint flows into helper
        return ResponseEntity.ok(executeQuery(builtQuery)); // taint reaches sink
    }

    private String buildQuery(String userInput) {
        // ❌ Taint propagates through string manipulation — still tainted
        return "SELECT * FROM orders WHERE product_name LIKE '%" + userInput + "%'";
    }

    private List<?> executeQuery(String sql) {
        // ❌ SINK: tainted SQL executed here — Sonar traces back to origin
        return jdbcTemplate.queryForList(sql);
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #3: Cross-class taint flow (hardest to detect manually)
    // SOURCE: HttpServletRequest header
    // FLOW:   Controller → TaintFlowService → FileSystemWriter (SINK)
    // Sonar Advanced SAST tracks taint across class boundaries
    // Sonar rule: S2083 (Path Traversal)
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/files/download")
    public ResponseEntity<String> downloadFile(HttpServletRequest request) {
        // ❌ SOURCE: user-controlled HTTP header
        String filename = request.getHeader("X-File-Name");
        // ❌ FLOW: taint crosses class boundary into service
        return ResponseEntity.ok(taintFlowService.readFile(filename));
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #4: Path Traversal via @RequestParam
    // SOURCE: @RequestParam "path"
    // SINK:   Files.readAllBytes(Paths.get(path)) — filesystem access
    // Sonar rule: S2083
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/files/read")
    public ResponseEntity<String> readFile(@RequestParam String path) {
        try {
            // ❌ SOURCE: path is user-controlled
            // ❌ SINK: used directly to read from filesystem
            // Attack: path = "../../etc/passwd"
            byte[] content = Files.readAllBytes(Paths.get("/app/data/" + path));
            return ResponseEntity.ok(new String(content));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error reading file");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #5: Command Injection
    // SOURCE: @RequestParam "filename"
    // SINK:   Runtime.exec() — OS command execution
    // Sonar rule: S2076
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/files/process")
    public ResponseEntity<String> processFile(@RequestParam String filename) {
        try {
            // ❌ SOURCE: filename is user-controlled
            // ❌ SINK: injected into shell command
            // Attack: filename = "file.txt; rm -rf /"
            Process process = Runtime.getRuntime().exec("convert /uploads/" + filename);
            return ResponseEntity.ok("Processing started for: " + filename);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Process failed");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #6: File Upload Path Traversal
    // SOURCE: MultipartFile.getOriginalFilename()
    // SINK:   Files.copy() to server filesystem
    // Sonar rule: S2083
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/files/upload")
    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
        try {
            // ❌ SOURCE: original filename from client — never trust this
            String originalName = file.getOriginalFilename();
            // ❌ SINK: tainted name used to write to filesystem
            // Attack: filename = "../../etc/cron.d/backdoor"
            Path target = Paths.get("/app/uploads/" + originalName);
            Files.copy(file.getInputStream(), target);
            return ResponseEntity.ok("Uploaded: " + originalName);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #7: Log Injection
    // SOURCE: @RequestParam "username"
    // SINK:   log.info() — log forging / log injection
    // Sonar rule: S5145
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestParam String username,
                                        @RequestParam String password) {
        // ❌ SOURCE: username and password are user-controlled
        // ❌ SINK: logged directly — attacker can inject fake log entries
        // Attack: username = "admin\nINFO: Successful admin login"
        log.info("Login attempt: username={}, password={}", username, password);
        return ResponseEntity.ok("Login processed");
    }

    // ──────────────────────────────────────────────────────────────
    // 🔴 TAINT #8: HTTP Response Splitting / Header Injection
    // SOURCE: @RequestParam "redirectUrl"
    // SINK:   HttpServletResponse header — response splitting
    // Sonar rule: S5167
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/redirect")
    public ResponseEntity<Void> redirect(@RequestParam String redirectUrl,
                                         jakarta.servlet.http.HttpServletResponse response) {
        // ❌ SOURCE: redirectUrl is user-controlled
        // ❌ SINK: set directly as Location header
        // Attack: inject \r\n to split HTTP response and add malicious headers
        response.setHeader("Location", redirectUrl);
        return ResponseEntity.status(302).build();
    }
}
