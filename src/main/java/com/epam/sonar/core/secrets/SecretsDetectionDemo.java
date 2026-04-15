package com.epam.sonar.core.secrets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════
 * 🔴 SECRETS DETECTION DEMO
 *
 * SonarQube detects hardcoded secrets using pattern matching,
 * entropy analysis, and known secret formats across:
 * - API keys (AWS, Stripe, SendGrid, GitHub, etc.)
 * - Database credentials
 * - JWT / OAuth tokens
 * - Private keys and certificates
 * - Cloud provider credentials
 *
 * Sonar rule: S2068, S6290, S6336, S6337, S6338, S6339
 * ══════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/secrets-demo")
@Slf4j
public class SecretsDetectionDemo {

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #1: AWS Credentials
    // Sonar rule: S6290
    // ──────────────────────────────────────────────────────────────
    private static final String AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";       // ❌ AWS key format
    private static final String AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"; // ❌
    private static final String AWS_REGION = "us-east-1";
    private static final String AWS_BUCKET = "my-prod-bucket";

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #2: Stripe Payment Keys
    // Sonar rule: S6336
    // ──────────────────────────────────────────────────────────────
    private static final String STRIPE_SECRET_KEY = "sk_live_4eC39HqLyjWDarjtT1zdp7dc"; // ❌ Live key!
    private static final String STRIPE_PUBLISHABLE = "pk_live_TYooMQauvdEDq54NiTphI7jx"; // ❌
    private static final String STRIPE_WEBHOOK_SECRET = "whsec_MbZPBYTEdCtMnQdBqhTQgVm9eMzHMrIp"; // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #3: GitHub / GitLab Tokens
    // Sonar rule: S6337
    // ──────────────────────────────────────────────────────────────
    private static final String GITHUB_TOKEN = "ghp_16C7e42F292c6912E7710c838347Ae178B4a";  // ❌
    private static final String GITHUB_APP_SECRET = "github_pat_11A4YXR3I0abc123XYZ456def789GHI"; // ❌
    private static final String GITLAB_TOKEN = "glpat-xxxxxxxxxxxxxxxxxxxx";                 // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #4: Google / GCP Credentials
    // Sonar rule: S6338
    // ──────────────────────────────────────────────────────────────
    private static final String GOOGLE_API_KEY = "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI";  // ❌
    private static final String GOOGLE_CLIENT_SECRET = "GOCSPX-abcdefghijklmnopqrstuvwxyz12";      // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #5: Database Credentials
    // Sonar rule: S2068
    // ──────────────────────────────────────────────────────────────
    private static final String PROD_DB_URL = "jdbc:postgresql://prod-db.company.com:5432/myapp";
    private static final String PROD_DB_USERNAME = "app_user";
    private static final String PROD_DB_PASSWORD = "Pr0d$ecur3P@ssw0rd!2024";  // ❌
    private static final String REDIS_PASSWORD = "redis-secret-password-xyz"; // ❌
    private static final String MONGO_URI = "mongodb://admin:p@ssw0rd123@mongo.prod:27017/db"; // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #6: JWT & OAuth Secrets
    // Sonar rule: S2068
    // ──────────────────────────────────────────────────────────────
    private static final String JWT_SECRET_KEY = "my-256-bit-secret-key-for-jwt-signing-2024!"; // ❌
    private static final String OAUTH_CLIENT_SECRET = "oauth2-client-secret-abcxyz123456789";        // ❌
    private static final String OAUTH_CLIENT_ID = "client_id_abcdefgh";
    private static final String SESSION_SECRET = "session-secret-key-never-share-this!";        // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #7: Third-party Service Keys
    // ──────────────────────────────────────────────────────────────
    private static final String SENDGRID_API_KEY = "SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // ❌
    private static final String TWILIO_AUTH_TOKEN = "AC1234567890abcdef1234567890abcdef";           // ❌
    private static final String SLACK_BOT_TOKEN = "xoxb-not-valid-token-but-pattern-matches";     // ❌
    private static final String OPENAI_API_KEY = "sk-proj-abcdefghijklmnopqrstuvwxyz123456";     // ❌

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #8: Private Keys (PEM format detected by Sonar)
    // Sonar rule: S6339
    // ──────────────────────────────────────────────────────────────
    private static final String PRIVATE_KEY = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEA2a2rwplBQLzHPZe5RJr9vA3q1lAAuSsRAPfgLOFkRFcGsGNL
            lWvFkVlGoGQJiMLzEKRSz0h6dBMFATUKExHJDnCdCbWqkMiFdJEBQFnGjT8OM1tA
            HbxFkQscFGQlkRBaNfFaXHPbXZiHoGKy1jZ0sZRLSKbgRvdIoYJa7xIgNpJhBqEH
            -----END RSA PRIVATE KEY-----
            """; // ❌ Private key in source code

    // ──────────────────────────────────────────────────────────────
    // 🔴 SECRETS #9: Secrets used at runtime (not just declared)
    // Sonar detects usage in method bodies too
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/payment/charge")
    public ResponseEntity<Map<String, String>> chargeCustomer(
            @RequestBody Map<String, Object> payload) {

        // ❌ Secret used to build authorization header — still hardcoded
        String authHeader = "Bearer " + STRIPE_SECRET_KEY;
        log.info("Charging customer with key: {}", STRIPE_SECRET_KEY); // ❌ Secret logged

        // Simulate API call
        return ResponseEntity.ok(Map.of(
                "status", "charged",
                "key_used", STRIPE_SECRET_KEY.substring(0, 10) + "..." // ❌ Partial secret exposed in response
        ));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        // ❌ Secrets exposed via API endpoint — config endpoint should never return secrets
        return ResponseEntity.ok(Map.of(
                "db_url", PROD_DB_URL,
                "db_password", PROD_DB_PASSWORD,  // ❌
                "jwt_secret", JWT_SECRET_KEY,    // ❌
                "aws_key", AWS_ACCESS_KEY_ID  // ❌
        ));
    }
}
