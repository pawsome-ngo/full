package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.incident.entity.Incident;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusDto {
    private Incident.IncidentStatus status;
}