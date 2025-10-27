package com.pawsome.rescue.features.incident.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteIncidentRequestDto {
    private boolean archive = true; // Default to archiving
}