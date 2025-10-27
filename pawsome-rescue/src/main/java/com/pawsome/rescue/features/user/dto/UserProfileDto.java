// File: pawsome-rescue/src/main/java/com/pawsome/rescue/features/user/dto/UserProfileDto.java
package com.pawsome.rescue.features.user.dto;

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String phoneNumber;
    private User.ExperienceLevel experienceLevel;
    private User.AvailabilityStatus availabilityStatus;
    private boolean hasMedicineBox;
    private int casesCompleted;
    private int hearts;
    private double distanceTraveled;

    // --- ✨ ADDED FIELDS ---
    private Boolean hasVehicle;
    private String vehicleType;
    private Boolean canProvideShelter;
    // --- End Added Fields ---
}