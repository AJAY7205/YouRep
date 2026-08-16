package com.learning.ytrep.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.learning.ytrep.model.User;

public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime createdAt);
    List<User> findByEmailVerifiedFalseAndReminderSentFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
