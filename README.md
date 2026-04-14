# Sonar AI / CodeFix — Full Demo Project

**Spring Boot 3.3** · **Java 24** · **Gradle** · **Lombok** · **H2** · **Spring Security**

> A deliberately flawed project designed to showcase SonarQube AI detection and AI CodeFix
> across all major issue categories. Every issue is annotated with its Sonar rule ID.

---

## Project Structure

```
sonar-demo/
├── build.gradle
├── settings.gradle
└── src/
    └── main/java/com/demo/sonar/
        ├── SonarDemoApplication.java
        ├── controller/
        │   ├── UserController.java       ← Security, validation, logging issues
        │   └── OrderController.java      ← NPE, null return issues
        ├── service/
        │   ├── UserService.java          ← Hardcoded secrets, NPE, resource leaks, perf
        │   ├── OrderService.java         ← Concurrency, exception handling, perf
        │   └── ReportService.java        ← Memory leaks, O(n²) operations
        ├── repository/
        │   └── UserRepository.java       ← SQL Injection (×3)
        ├── scheduler/
        │   └── CleanupScheduler.java     ← Lock leak, resource leak, InterruptedException
        ├── util/
        │   └── SecurityUtil.java         ← Weak crypto, insecure random, TODO
        ├── model/
        │   ├── User.java                 ← EAGER fetch (N+1 performance)
        │   └── Order.java
        └── config/
            └── SecurityConfig.java       ← CSRF disabled, no auth
```

---

## Issue Map — All 30+ Issues

### 🔴 SQL Injection (Sonar: S2077)
| File | Line | Description |
|------|------|-------------|
| `UserRepository.java` | searchByUsername | JPQL string concatenation — username param |
| `UserRepository.java` | findByRole | JPQL string concatenation — role param |
| `UserRepository.java` | searchByEmail | JPQL LIKE with email param interpolated |

### 🔴 Hardcoded Secrets (Sonar: S2068)
| File | Issue |
|------|-------|
| `UserService.java` | `ADMIN_PASSWORD = "admin123"` |
| `UserService.java` | `DB_PASSWORD = "superSecret@db!9"` |
| `UserService.java` | `API_SECRET_KEY = "sk-prod-abc123..."` |
| `UserService.java` | `JWT_SECRET = "myJwtSecret2024..."` |
| `SecurityUtil.java` | `SECRET = "hardcoded-jwt-secret-2024"` |

### 🔴 Null Pointer / Exception Handling (Sonar: S2259, S2221, S108, S1166)
| File | Issue |
|------|-------|
| `UserService.java` | `orElse(null)` + direct `.getUsername()` call |
| `UserService.java` | `.get()` on Optional without check |
| `UserService.java` | Generic `Exception` caught and swallowed |
| `UserService.java` | Empty catch block on deleteUser |
| `UserService.java` | Exception cause lost on re-throw |
| `OrderController.java` | `getOrdersForUser()` returns null → NPE on `.stream()` |
| `OrderService.java` | `finally` block throws, masking original exception |
| `OrderService.java` | Catching `Throwable` in business logic |

### 🔴 Resource Management / Leaks (Sonar: S2095)
| File | Issue |
|------|-------|
| `UserService.java` | `Connection`, `Statement`, `ResultSet` not in try-with-resources |
| `UserService.java` | `InputStream` / `BufferedReader` not closed on exception |
| `UserService.java` | `FileWriter` only closed on happy path |
| `CleanupScheduler.java` | `FileWriter` not in try-with-resources |

### 🔴 Concurrency Issues (Sonar: S2885, S2142, S2222, S2886)
| File | Issue |
|------|-------|
| `OrderService.java` | `HashMap` used in Spring singleton (not thread-safe) |
| `OrderService.java` | `ArrayList` used in Spring singleton (not thread-safe) |
| `OrderService.java` | Non-atomic read-modify-write on `totalOrdersProcessed` |
| `OrderService.java` | `InterruptedException` caught without re-interrupting thread |
| `CleanupScheduler.java` | `ReentrantLock` acquired outside try-finally — deadlock risk |
| `CleanupScheduler.java` | `InterruptedException` swallowed in scheduler |

### 🔴 Performance Bottlenecks
| File | Issue |
|------|-------|
| `UserService.java` | String concatenation in loop — use `StringBuilder` |
| `UserService.java` | `findAll()` to count active users — use COUNT query |
| `UserService.java` | `@Cacheable` on mutation method |
| `OrderService.java` | N+1 query — separate DB call per userId in loop |
| `OrderService.java` | `new String(...)` — unnecessary object creation |
| `OrderService.java` | `&` instead of `&&` — eager evaluation, NPE risk |
| `ReportService.java` | `findAll()` loads entire table into heap |
| `ReportService.java` | `List.contains()` in loop — O(n²), use Set |
| `User.java` | `FetchType.EAGER` on collection — N+1 on every User load |

### 🔴 Memory Leaks
| File | Issue |
|------|-------|
| `ReportService.java` | Static `HashMap` cache grows unboundedly — never evicted |
| `ReportService.java` | Static `ArrayList` audit log — never cleared |

### 🔴 Security / Cryptography (Sonar: S4790, S2070, S2245)
| File | Issue |
|------|-------|
| `SecurityUtil.java` | MD5 used for password hashing |
| `SecurityUtil.java` | SHA-1 used for checksums |
| `SecurityUtil.java` | `java.util.Random` used for token generation |
| `SecurityUtil.java` | String comparison with `==` instead of `.equals()` |
| `SecurityUtil.java` | TODO left in production encryption method |
| `SecurityConfig.java` | CSRF disabled |
| `SecurityConfig.java` | All endpoints permitted without authentication |

### 🔴 Code Smells (Sonar: S3776, S1854, S1481, S4488)
| File | Issue |
|------|-------|
| `UserService.java` | 7-level deep nesting — cognitive complexity too high |
| `UserService.java` | Dead store: `unusedCounter`, `unusedMessage` |
| `ReportService.java` | Dead store: `taxRate` variable |
| `ReportService.java` | Returns `null` instead of `Optional` or empty list |
| `UserController.java` | `@RequestMapping` used instead of `@GetMapping` |

---

## Quick Start

```bash
# Run the application
./gradlew bootRun

# Run tests with coverage
./gradlew test jacocoTestReport

# Run SonarQube analysis (configure token first)
./gradlew sonar
```

---

## SonarQube Cloud Setup

1. Create a project on [sonarcloud.io](https://sonarcloud.io)
2. Copy your project key and token
3. Update `build.gradle`:
```groovy
sonar {
    properties {
        property 'sonar.projectKey', 'YOUR_PROJECT_KEY'
        property 'sonar.token',      'YOUR_TOKEN'
    }
}
```
4. Run `./gradlew sonar`
5. View issues on SonarQube Cloud dashboard
6. Click any issue → AI CodeFix tab → Generate AI Fix

---

## Demo Flow

### Step 1 — IDE (Real-time)
Open in IntelliJ / VS Code with **SonarQube for IDE** — issues highlight immediately as you navigate files.

### Step 2 — Dashboard
Push → trigger analysis → show the full issues dashboard grouped by category.

### Step 3 — AI CodeFix
Pick a high-impact issue (SQL Injection or concurrency) → AI CodeFix → Generate → show diff.

### Step 4 — Quality Gate
Apply fixes → rerun analysis → show Quality Gate passing → AI Code Assurance badge.
