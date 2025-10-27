package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.incident.entity.Incident;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class IncidentDto {
    private Long id;
    private String informerName;
    private String contactNumber;
    private Double latitude;
    private Double longitude;
    private String location;
    private Incident.AnimalType animalType;
    private String description;
    private Incident.IncidentStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime lastUpdated;

    // --- ADD THIS LINE BACK ---
    private List<IncidentMediaDto> mediaFiles; // The actual list of media DTOs
    // --- END ADDITION ---

    private int mediaFileCount; // Keep the count as well
    private int caseCount;
    private List<InterestedUserDto> interestedUsers;
    private Long assignedByUserId;
}