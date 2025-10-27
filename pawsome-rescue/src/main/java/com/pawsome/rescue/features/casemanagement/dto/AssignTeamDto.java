package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AssignTeamDto {
    private List<Long> userIds;
}