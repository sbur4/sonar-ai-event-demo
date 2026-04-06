package com.epam.sonar.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

@Component
@Slf4j
public class SecurityUtil {

    // ══════════════════════════════════════════════════════
    // 🔴 ISSUE CATEGORY: SECURITY / CRYPTOGRAPHY
    // ══════════════════════════════════════════════════════

    // 🔴 S2068 - Hardcoded secret key
    private static final String SECRET = "hardcoded-jwt-secret-2024";

    // 🔴 S2245 - Using java.util.Random for security — not cryptographically secure
    private final Random random = new Random(); // ❌ Use SecureRandom for tokens

    // 🔴 S4790 - MD5 is a weak hashing algorithm — broken for security purposes
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5"); // ❌ Use BCrypt or Argon2
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // 🔴 S1166 - Exception caught, cause not preserved in re-throw
            throw new RuntimeException("Hashing failed"); // ❌ original cause lost
        }
    }

    // 🔴 S2245 - Non-secure random used for token generation
    public String generateToken(String userId) {
        // ❌ Predictable — attacker can guess tokens
        int token = random.nextInt(999999);
        return userId + "-" + token;
    }

    // 🔴 S2070 - SHA-1 is also cryptographically weak
    public String generateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1"); // ❌ Use SHA-256 minimum
            byte[] hash = md.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return null; // ❌ Returning null — callers not expecting it
        }
    }

    // 🔴 S2277 - Comparing strings with == instead of .equals()
    public boolean validateRole(String role) {
        return role == "ADMIN" || role == "USER"; // ❌ Reference comparison, not value
    }

    // 🔴 S1135 - TODO left in production code
    public String encryptData(String data) {
        // TODO: implement real encryption before production release ❌
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
}
