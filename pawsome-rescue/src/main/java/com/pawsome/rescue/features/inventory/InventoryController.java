package com.pawsome.rescue.features.inventory;

import com.pawsome.rescue.features.inventory.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // --- Dashboard ---
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<InventoryDashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(inventoryService.getDashboardStats());
    }

    // --- Requisition Management ---
    @GetMapping("/requisitions")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<RequisitionDto>> getRequisitionsByStatus(@RequestParam String status) {
        return ResponseEntity.ok(inventoryService.getRequisitionsByStatus(status));
    }

    @PostMapping("/requisitions/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> approveRequisition(@PathVariable Long id) {
        inventoryService.approveRequisition(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requisitions/{id}/deny")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> denyRequisition(@PathVariable Long id) {
        inventoryService.denyRequisition(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requisitions/{id}/dispatch")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> dispatchRequisition(@PathVariable Long id) {
        inventoryService.dispatchRequisition(id);
        return ResponseEntity.ok().build();
    }

    // --- Inventory Item Management ---
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItemDto>> getAllItems() {
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<InventoryItemDto> createItem(@RequestBody InventoryItemDto itemDto) {
        return ResponseEntity.ok(inventoryService.createInventoryItem(itemDto));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<InventoryItemDto> updateItem(@PathVariable Long id, @RequestBody InventoryItemDto itemDto) {
        return ResponseEntity.ok(inventoryService.updateInventoryItem(id, itemDto));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        inventoryService.deleteInventoryItem(id);
        return ResponseEntity.ok().build();
    }

    // --- Category Management ---
    @GetMapping("/categories")
    public ResponseEntity<List<ItemCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(inventoryService.getAllItemCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ItemCategoryDto> createCategory(@RequestBody ItemCategoryDto categoryDto) {
        return ResponseEntity.ok(inventoryService.createItemCategory(categoryDto));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ItemCategoryDto> updateCategory(@PathVariable Long id, @RequestBody ItemCategoryDto categoryDto) {
        return ResponseEntity.ok(inventoryService.updateItemCategory(id, categoryDto));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        inventoryService.deleteItemCategory(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/items/{id}/usage")
    @PreAuthorize("hasAnyAuthority('ROLE_INVENTORY_MANAGER', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ItemInUseDto> getItemUsage(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getItemUsage(id));
    }
}