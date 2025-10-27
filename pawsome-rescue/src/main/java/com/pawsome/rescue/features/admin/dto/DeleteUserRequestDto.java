package com.pawsome.rescue.features.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteUserRequestDto {
    private boolean notifyUsers = false; // Default to false
}