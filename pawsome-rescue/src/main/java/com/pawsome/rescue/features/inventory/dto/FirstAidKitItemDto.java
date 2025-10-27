package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirstAidKitItemDto {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Integer quantity;
    private boolean personallyProcured;
}