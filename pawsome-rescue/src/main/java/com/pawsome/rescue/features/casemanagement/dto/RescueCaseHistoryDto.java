package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class RescueCaseHistoryDto {
    private Long caseId;
    private String teamName;
    private String assignedBy;
    private List<TeamMemberDto> teamMembers;
    private String resolutionNotes;
    private LocalDateTime closedAt;
}