package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.casemanagement.dto.RescueCaseHistoryDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IncidentHistoryDto {
    private List<RescueCaseHistoryDto> cases;
}