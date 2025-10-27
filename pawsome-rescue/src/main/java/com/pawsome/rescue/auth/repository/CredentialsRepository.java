package com.pawsome.rescue.auth.repository;

import com.pawsome.rescue.auth.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    Optional<Credentials> findByUsername(String username);
    Optional<Credentials> findByUserId(Long userId); // <-- ADD THIS LINE
}