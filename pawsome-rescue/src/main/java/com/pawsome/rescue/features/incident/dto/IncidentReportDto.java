package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.incident.entity.Incident.AnimalType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidentReportDto {
    private String informerName;
    private String contactNumber;
    private Double latitude;
    private Double longitude;
    private String location; // New field
    private AnimalType animalType;
    private String description;
}