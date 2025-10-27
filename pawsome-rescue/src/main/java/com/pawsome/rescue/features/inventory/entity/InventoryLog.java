package com.pawsome.rescue.features.inventory.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // User who requested or used the item

    @ManyToOne
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy; // Admin who approved the request

    @Column(nullable = false)
    private String action; // e.g., "REQUESTED", "APPROVED", "DENIED", "ADDED_TO_STOCK"

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}