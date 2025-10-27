package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryItemDto {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String unit;
}