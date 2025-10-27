package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamMemberDto {
    private Long userId;
    private String firstName;
    private String fullName;
}