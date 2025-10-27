package com.pawsome.rescue.features.incident.repository;

import com.pawsome.rescue.features.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List; // Import List

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    // New method to find incidents by their status
    List<Incident> findByStatus(Incident.IncidentStatus status);

    @Modifying
    @Query(value = "ALTER TABLE incidents AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement(); // Ensure this method exists
}