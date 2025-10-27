package com.pawsome.rescue.features.inventory;

import com.pawsome.rescue.features.inventory.dto.RequisitionDto;
import com.pawsome.rescue.features.inventory.dto.RequisitionItemDto;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requisitions")
public class RequisitionController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    // Add this new endpoint
    @GetMapping("/my-requests")
    public ResponseEntity<List<RequisitionDto>> getMyOpenRequisitions(Authentication authentication) {
        String username = authentication.getName();
        Long userId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        return ResponseEntity.ok(inventoryService.getOpenRequisitionsForUser(userId));
    }

    @PostMapping
    public ResponseEntity<RequisitionDto> createRequisition(Authentication authentication, @RequestBody List<RequisitionItemDto> itemDtos) {
        String username = authentication.getName();
        Long userId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        try {
            RequisitionDto newRequisition = inventoryService.createRequisition(userId, itemDtos);
            return ResponseEntity.ok(newRequisition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeReceipt(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        Long userId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        inventoryService.acknowledgeReceipt(id, userId);
        return ResponseEntity.ok().build();
    }
}