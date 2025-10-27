// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/casemanagement/dto/InitiateCaseDto.java
package com.pawsome.rescue.features.casemanagement.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class InitiateCaseDto {
    private List<Long> participatingUserIds;
}