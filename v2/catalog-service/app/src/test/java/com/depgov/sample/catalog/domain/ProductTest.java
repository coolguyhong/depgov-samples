package com.depgov.sample.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void normalizesSkuAndNameAndAcceptsFreeProduct() {
        Product product = new Product(" cat-001 ", "  Travel   Mug ", 0, "mug.png");
        assertEquals("CAT-001", product.getSku());
        assertEquals("Travel Mug", product.getName());
        assertEquals(0, product.getPrice());
    }

    @Test
    void acceptsMaximumColumnLengths() {
        Product product = new Product("A".repeat(32), "B".repeat(100), Long.MAX_VALUE, "C".repeat(120));
        assertEquals(32, product.getSku().length());
        assertEquals(100, product.getName().length());
        assertEquals(120, product.getAttachmentName().length());
    }

    @Test
    void rejectsInvalidSku() {
        for (String sku : new String[]{null, " ", "A/B", "A B", "-A", "A".repeat(33)}) {
            assertThrows(IllegalArgumentException.class, () -> new Product(sku, "Name", 1, "a.png"));
        }
    }

    @Test
    void rejectsBlankAndLongName() {
        for (String name : new String[]{null, " \t ", "N".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> new Product("A", name, 1, "a.png"));
        }
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Product("A", "Name", -1, "a.png"));
    }

    @Test
    void rejectsBlankAndLongAttachmentName() {
        for (String name : new String[]{null, " ", "N".repeat(121)}) {
            assertThrows(IllegalArgumentException.class, () -> new Product("A", "Name", 1, name));
        }
    }
}
