// src/main/java/com/bingeboxed/profiles/repository/UserProfileRepository.java
package com.bingeboxed.profiles.repository;

import com.bingeboxed.profiles.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
}