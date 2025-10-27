package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamDetailsDto {
    private String teamName;
    private List<TeamMemberDto> teamMembers;
    private String assignedBy;
}