package com.epam.sonar.data.repository;

import com.epam.sonar.data.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findAllActive();
}

/**
 * ══════════════════════════════════════════════════════
 * 🔴 ISSUE CATEGORY: SQL INJECTION
 * ══════════════════════════════════════════════════════
 */
@Repository
class UserSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // 🔴 S2077 - SQL Injection: user-controlled input concatenated into JPQL
    @SuppressWarnings("unchecked")
    public List<User> searchByUsername(String username) {
        String query = "SELECT u FROM User u WHERE u.username = '" + username + "'";
        return entityManager.createQuery(query).getResultList();
    }

    // 🔴 S2077 - SQL Injection: role parameter directly interpolated
    @SuppressWarnings("unchecked")
    public List<User> findByRole(String role) {
        return entityManager
                .createQuery("SELECT u FROM User u WHERE u.role = '" + role + "'")
                .getResultList();
    }

    // 🔴 S2077 - SQL Injection: email search vulnerable to injection
    @SuppressWarnings("unchecked")
    public List<User> searchByEmail(String email) {
        String jpql = "SELECT u FROM User u WHERE u.email LIKE '%" + email + "%'";
        return entityManager.createQuery(jpql).getResultList();
    }
}
