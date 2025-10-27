package com.pawsome.rescue.auth.repository;

import com.pawsome.rescue.auth.entity.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingUserRepository extends JpaRepository<PendingUser, Long> {
    Optional<PendingUser> findByUsername(String username);
    Optional<PendingUser> findByPhoneNumber(String phoneNumber);
}