package com.pawsome.rescue.features.casemanagement.dto;

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailableVolunteerDto {
    private Long userId;
    private String firstName;
    private Boolean hasVehicle;
    private Boolean hasMedicineBox;
    private User.ExperienceLevel experienceLevel;
    private Boolean hasShownInterest;
    private Boolean isEngagedInActiveCase;
    private Double distanceFromIncident;
    private Boolean hasPreviouslyWorkedOnIncident;
}