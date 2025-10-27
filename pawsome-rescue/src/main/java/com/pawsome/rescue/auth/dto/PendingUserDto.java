package com.pawsome.rescue.auth.dto;

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingUserDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String motivation;
    private Boolean hasVehicle;
    private String vehicleType;
    private Boolean canProvideShelter;
    private Boolean hasMedicineBox;
    private User.ExperienceLevel experienceLevel;
}