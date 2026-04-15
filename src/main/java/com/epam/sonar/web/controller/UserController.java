package com.epam.sonar.web.controller;

import com.epam.sonar.core.service.UserService;
import com.epam.sonar.data.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    // 🔴 S2259 - Chained NPE risk from service returning null
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id)); // ❌ Can be NPE
    }

    // 🔴 S4684 - Mass assignment: Entity accepted directly, no DTO
    // 🔴 S5122 - No input validation with @Valid
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // ❌ No @Valid — any field including 'role' can be set by caller
        // ❌ Caller can set role=ADMIN bypassing business rules
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // 🔴 S5145 - Logging sensitive data (password in plain text)
    // 🔴 S2068 - Hard-coded credentials used for comparison
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username,
                                        @RequestParam String password) {
        // ❌ Password logged to log file in plain text
        log.info("Login attempt — username: {}, password: {}", username, password);

        if (userService.isAdmin(password)) {
            return ResponseEntity.ok("Welcome, admin");
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }

    // 🔴 S2083 - Path traversal: user-controlled filename used to build file path
    @GetMapping("/export")
    public ResponseEntity<String> exportUserFile(@RequestParam String filename) {
        // ❌ Attacker can use "../../../etc/passwd" to traverse filesystem
        File file = new File("/app/exports/" + filename);
        return ResponseEntity.ok("Exported to: " + file.getAbsolutePath());
    }

    // 🔴 S4488 - @RequestMapping used instead of specific @GetMapping (code smell)
    @RequestMapping("/report")
    public ResponseEntity<String> getUserReport() {
        // ❌ Accepts all HTTP methods — should be @GetMapping
        return ResponseEntity.ok(userService.buildUserReport());
    }
}
