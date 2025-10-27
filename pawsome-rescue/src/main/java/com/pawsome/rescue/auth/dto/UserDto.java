package com.pawsome.rescue.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor    // <-- ADD THIS ANNOTATION
@AllArgsConstructor // <-- ADD THIS ANNOTATION
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
}