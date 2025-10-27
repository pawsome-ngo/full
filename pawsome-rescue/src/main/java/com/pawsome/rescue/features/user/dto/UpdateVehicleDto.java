// File: pawsome-rescue/src/main/java/com/pawsome/rescue/features/user/dto/UpdateVehicleDto.java
package com.pawsome.rescue.features.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVehicleDto {
    private Boolean hasVehicle;
    private String vehicleType;
}