package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class FirstAidKitDto {
    private Long id;
    private Long userId;
    private List<FirstAidKitItemDto> items;
}