package com.torii.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Access to the {@code users} table. */
public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
