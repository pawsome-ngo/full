package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AssignTeamResponseDto {
    private Long caseId;
    private Long incidentId;
    private String teamName;
    private String chatGroupId;
    private List<TeamMemberDto> teamMembers;
}