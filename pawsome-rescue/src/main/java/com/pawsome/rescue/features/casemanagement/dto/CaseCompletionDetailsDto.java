package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseCompletionDetailsDto {
    private String resolutionNotes;
    private Double finalLatitude;
    private Double finalLongitude;
}