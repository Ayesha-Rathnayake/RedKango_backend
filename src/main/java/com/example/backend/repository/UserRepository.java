package com.example.backend.repository;

import com.example.backend.domain.Role;
import com.example.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByRolesContainingAndDeletedFalse(Role role);

    long countByRolesContainingAndDeletedFalse(Role role);

    long countByRolesContainingAndEnabledAndDeletedFalse(Role role, boolean enabled);

    List<User> findByRolesContaining(Role role);
}