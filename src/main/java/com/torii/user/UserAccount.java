package com.torii.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * User account ({@code users} table).
 *
 * <p>The password is NEVER stored in plain text, only its BCrypt hash (one way, login
 * compares it with {@code PasswordEncoder.matches}). The plan is stored as a plain
 * string ("FREE", "PRO", "BUSINESS") and mapped to {@link Plan} when needed.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String plan;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** No-arg constructor required by JPA, don't use directly. */
    protected UserAccount() {}

    public UserAccount(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.plan = "FREE"; // everyone starts on the free plan
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getPlan() {
        return plan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Changes the account plan (no payments yet, called from POST /api/me/plan). */
    public void changePlan(Plan newPlan) {
        this.plan = newPlan.name();
    }
}
