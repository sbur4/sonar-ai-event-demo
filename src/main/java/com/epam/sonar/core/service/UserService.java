package com.epam.sonar.core.service;

import com.epam.sonar.data.model.User;
import com.epam.sonar.data.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserService {

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: HARDCODED SECRETS
    // ══════════════════════════════════════════════════════

    // 🔴 S2068 - Hard-coded credentials
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String DB_PASSWORD = "superSecret@db!9";
    private static final String API_SECRET_KEY = "sk-prod-abc123xyz-LIVE";
    private static final String JWT_SECRET = "myJwtSecret2024!hardcoded";
    private static final String DB_URL = "jdbc:postgresql://prod-db:5432/users";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: NULL POINTER / EXCEPTION HANDLING
    // ══════════════════════════════════════════════════════

    // 🔴 S2259 - NPE: orElse(null) followed by direct field access
    public User getUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        log.info("Found user: {}", user.getUsername()); // ❌ NPE if not found
        return user;
    }

    // 🔴 S2259 - NPE: findByUsername may return empty, .get() without check
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).get(); // ❌ NoSuchElementException
    }

    // 🔴 S2221 - Catching generic Exception, swallowing it
    public User createUser(User user) {
        try {
            user.setCreatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } catch (Exception e) {
            // ❌ Exception swallowed — caller has no idea what went wrong
            log.error("Error saving user");
            return null;
        }
    }

    // 🔴 S108 - Empty catch block
    public void deleteUser(Long id) {
        try {
            userRepository.deleteById(id);
        } catch (Exception e) {
            // ❌ Completely empty — exception silently ignored
        }
    }

    // 🔴 S1166 - Exception caught but original not rethrown or logged with cause
    public User updateUser(Long id, User updated) {
        try {
            User existing = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existing.setEmail(updated.getEmail());
            existing.setRole(updated.getRole());
            return userRepository.save(existing);
        } catch (RuntimeException e) {
            // ❌ Original exception cause lost — not logged, not rethrown properly
            throw new RuntimeException("Update failed"); // original cause swallowed
        }
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: RESOURCE MANAGEMENT / MEMORY LEAKS
    // ══════════════════════════════════════════════════════

    // 🔴 S2095 - Resource leak: Connection, Statement, ResultSet never closed
    public List<String> getUsernamesFromLegacyDb() {
        List<String> usernames = new ArrayList<>();
        try {
            // ❌ BAD: Resources not in try-with-resources — leak on exception
            Connection conn = DriverManager.getConnection(DB_URL, "admin", DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username FROM users");
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            // ❌ If exception thrown above, conn/stmt/rs are never closed
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("DB error: {}", e.getMessage());
        }
        return usernames;
    }

    // 🔴 S2095 - Resource leak: InputStream and BufferedReader not closed
    public String fetchExternalUserData(String userId) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL("https://api.example.com/users/" + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // ❌ InputStream never closed if exception occurs during read
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            // ❌ No finally block, no try-with-resources
        } catch (IOException e) {
            log.error("Error fetching user data");
        }
        return result.toString();
    }

    // 🔴 S2095 - File resource leak
    public void exportUsersToFile(String filePath) {
        try {
            // ❌ FileWriter not closed if exception during write
            FileWriter writer = new FileWriter(filePath);
            List<User> users = userRepository.findAll();
            for (User u : users) {
                writer.write(u.getUsername() + "," + u.getEmail() + "\n");
            }
            writer.close(); // ❌ Only closed on happy path
        } catch (IOException e) {
            log.error("Export failed: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: PERFORMANCE BOTTLENECKS
    // ══════════════════════════════════════════════════════

    // 🔴 S3984 / Performance: String concatenation in loop (use StringBuilder)
    public String buildUserReport() {
        List<User> users = userRepository.findAll();
        String report = ""; // ❌ Mutable String in loop = O(n²) memory allocations
        for (User u : users) {
            report += "User: " + u.getUsername() + " | Email: " + u.getEmail() + "\n"; // ❌
        }
        return report;
    }

    // 🔴 Performance: Loading all records into memory for simple count
    public int countActiveUsers() {
        // ❌ Loads ALL users into memory just to count — should use COUNT query
        return (int) userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .count();
    }

    // 🔴 S3824 / Performance: @Cacheable on a method that has side effects
    @Cacheable("users")
    public User createAndCacheUser(User user) {
        // ❌ Caching a mutation method — misleads callers, wrong cache semantics
        return userRepository.save(user);
    }

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: CODE SMELLS & MAINTAINABILITY
    // ══════════════════════════════════════════════════════

    // 🔴 S3776 - Cognitive complexity too high (deeply nested logic)
    public String processUser(User u, boolean active, int score) {
        String res = "";
        if (u != null) {
            if (active) {
                if (score > 0) {
                    if (u.getEmail() != null) {
                        if (u.getEmail().contains("@")) {
                            if (u.getRole() != null) {
                                if (u.getRole().equals("ADMIN")) {
                                    res = "admin_valid:" + u.getUsername();
                                } else {
                                    res = "user_valid:" + u.getUsername();
                                }
                            } else {
                                res = "no_role";
                            }
                        } else {
                            res = "invalid_email";
                        }
                    } else {
                        res = "no_email";
                    }
                } else {
                    res = "bad_score";
                }
            } else {
                res = "inactive";
            }
        }
        return res; // ❌ 7 levels deep, poor names, no early returns
    }

    // 🔴 S2068 - Password comparison with hardcoded value
    public boolean isAdmin(String password) {
        return ADMIN_PASSWORD.equals(password); // ❌
    }

    // 🔴 S1854 - Dead store: variable assigned but never used
    public List<User> getActiveUsers() {
        List<User> allUsers = userRepository.findAll();
        int unusedCounter = 0; // ❌ Dead store — assigned but never read
        String unusedMessage = "Processing users..."; // ❌ Dead store
        return allUsers.stream()
                .filter(User::isActive)
                .toList();
    }
}
