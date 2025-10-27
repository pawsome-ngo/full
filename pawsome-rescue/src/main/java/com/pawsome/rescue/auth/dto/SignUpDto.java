package com.pawsome.rescue.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpDto {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String motivation;
    private Boolean hasVehicle;
    private String vehicleType;
    private Boolean canProvideShelter;
    private Boolean hasMedicineBox;
    private Double latitude;
    private Double longitude;
}