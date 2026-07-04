package com.torii.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cuenta de usuario (tabla {@code users}).
 *
 * <p>La contraseña NUNCA se guarda en claro: aquí solo vive su hash BCrypt
 * (irreversible; en el login se compara con {@code PasswordEncoder.matches}).
 * El plan es un string simple ("FREE", "PRO", "BUSINESS") hasta que la fase de
 * cuotas lo convierta en algo más rico.
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

    /** Constructor vacío exigido por JPA; no usar directamente. */
    protected UserAccount() {}

    public UserAccount(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.plan = "FREE"; // todo el mundo empieza en el plan gratuito
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

    /** Cambia el plan de la cuenta (hoy sin pagos; lo llama POST /api/me/plan). */
    public void changePlan(Plan newPlan) {
        this.plan = newPlan.name();
    }
}
