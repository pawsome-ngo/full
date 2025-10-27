package com.pawsome.rescue.features.incident.repository;

import com.pawsome.rescue.features.incident.entity.IncidentMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentMediaRepository extends JpaRepository<IncidentMedia, Long> {
    List<IncidentMedia> findByIncidentId(Long incidentId); // Add this method
}