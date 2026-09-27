package com.torii.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Tokens issued before this change are rejected (see SecurityConfig). */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

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

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    /**
     * Swaps the password hash. The change time goes into every JWT (see JwtService),
     * truncated to millis so it survives the round trip through the DB unchanged.
     */
    public void changePassword(String newHash, Instant now) {
        this.passwordHash = newHash;
        this.passwordChangedAt = now.truncatedTo(ChronoUnit.MILLIS);
    }

    /** Value of the JWT "pwc" claim: 0 until the password is changed for the first time. */
    public long passwordVersion() {
        return passwordChangedAt == null ? 0 : passwordChangedAt.toEpochMilli();
    }

    /** Changes the account plan (no payments yet, called from POST /api/me/plan). */
    public void changePlan(Plan newPlan) {
        this.plan = newPlan.name();
    }
}
