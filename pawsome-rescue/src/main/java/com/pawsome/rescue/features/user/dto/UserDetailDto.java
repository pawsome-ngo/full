package com.pawsome.rescue.features.user.dto;

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserDetailDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime joinedSince;
    private User.AvailabilityStatus availabilityStatus;
    private User.Position position;
    private User.ExperienceLevel experienceLevel;
    private List<String> roles;
    private Boolean hasMedicineBox;
}