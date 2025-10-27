package com.pawsome.rescue.features.user.dto;

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummaryDto {
    private Long id;
    private String fullName;
    private User.AvailabilityStatus availabilityStatus;
    private User.Position position;
}