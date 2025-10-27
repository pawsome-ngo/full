package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryRequestDto {
    private Long id;
    private Long userId;
    private String userName;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Integer quantity;
    private String status;
    private LocalDateTime requestedAt;
}