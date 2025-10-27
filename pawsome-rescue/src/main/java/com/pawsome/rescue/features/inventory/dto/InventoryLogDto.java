package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryLogDto {
    private Long id;
    private String itemName;
    private String userName;
    private String approvedByUserName;
    private String action;
    private Integer quantity;
    private LocalDateTime timestamp;
}