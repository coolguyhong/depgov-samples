package com.depgov.sample.inventory.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @Column(length = 32, nullable = false)
    private String sku;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, String name, int quantity) {
        this.sku = sku;
        this.name = name;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
