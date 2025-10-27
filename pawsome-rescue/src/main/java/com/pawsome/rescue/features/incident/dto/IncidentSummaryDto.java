package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.incident.entity.Incident;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class IncidentSummaryDto {
    private Long id;
    private String informerName;
    private String location;
    private Incident.AnimalType animalType;
    private Incident.IncidentStatus status;
    private LocalDateTime lastUpdated;
    private int caseCount;
    private Long assignedByUserId;
}