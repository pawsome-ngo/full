package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryDashboardStatsDto {
    private long pendingRequisitions;
    private long readyForPickup;
    private long lowStockItems;
}