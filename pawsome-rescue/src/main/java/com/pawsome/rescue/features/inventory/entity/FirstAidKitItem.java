package com.pawsome.rescue.features.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "first_aid_kit_items")
@Getter
@Setter
public class FirstAidKitItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "first_aid_kit_id", nullable = false)
    private FirstAidKit firstAidKit;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "personally_procured", nullable = false)
    private boolean personallyProcured = false;
}