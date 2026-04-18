// src/main/java/com/bingeboxed/auth/repository/AuthUserRepository.java
package com.bingeboxed.auth.repository;

import com.bingeboxed.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
}