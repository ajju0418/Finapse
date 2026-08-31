package com.finapse.repository;

import com.finapse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * The legacy single-tenant row created before authentication existed.
     * Claimed by the first account that registers so pre-existing financial
     * data stays reachable.
     */
    @Query("SELECT u FROM User u WHERE u.email IS NULL OR u.passwordHash IS NULL")
    Optional<User> findUnclaimedLegacyUser();
}
