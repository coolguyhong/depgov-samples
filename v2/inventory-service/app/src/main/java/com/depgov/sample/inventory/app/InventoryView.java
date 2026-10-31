package com.depgov.sample.inventory.app;

public record InventoryView(String sku, String name, int quantity, String status, String stockCard) {
}
