package com.pawsome.rescue.features.archive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentArchiveRepository extends JpaRepository<IncidentArchive, Long> {
    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE incident_archive AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}