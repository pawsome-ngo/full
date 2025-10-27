package com.pawsome.rescue.features.inventory;

import com.pawsome.rescue.features.inventory.dto.FirstAidKitDto;
import com.pawsome.rescue.features.inventory.dto.FirstAidKitItemDto;
import com.pawsome.rescue.features.inventory.dto.UpdateFirstAidKitItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/first-aid-kit")
public class FirstAidKitController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/{userId}")
    public ResponseEntity<FirstAidKitDto> getFirstAidKit(@PathVariable Long userId) {
        return ResponseEntity.ok(inventoryService.getFirstAidKitByUserId(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addItemToKit(@PathVariable Long userId, @RequestBody FirstAidKitItemDto itemDto) {
        try {
            if (itemDto.isPersonallyProcured()) {
                // MODIFIED: Corrected method name
                return ResponseEntity.ok(inventoryService.addPersonallyProcuredItem(userId, itemDto));
            } else {
                // This logic path might need to be adjusted for the new requisition system.
                // For now, let's assume it might create a single-item requisition.
                // This is a placeholder for a future decision on how to handle this.
                return ResponseEntity.badRequest().body("Please use the new requisition system to request items from inventory.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<Void> removeItemFromKit(@PathVariable Long userId, @PathVariable Long itemId) {
        inventoryService.removeItemFromFirstAidKit(userId, itemId);
        return ResponseEntity.ok().build();
    }
    // ADD THIS NEW ENDPOINT
    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<Void> updateItemInKit(@PathVariable Long userId, @PathVariable Long itemId, @RequestBody UpdateFirstAidKitItemDto dto) {
        try {
            inventoryService.updateItemInFirstAidKit(userId, itemId, dto.getQuantity());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}