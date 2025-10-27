package com.pawsome.rescue.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordDto {
    private String oldPassword;
    private String newPassword;
}