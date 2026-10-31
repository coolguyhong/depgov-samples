package com.depgov.sample.inventory.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.depgov.sample.inventory.domain.InventoryPolicy;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;

class InventoryPolicyTest {
    @Test
    void normalizesSkuAndPreservesMaximumLength() {
        assertEquals("SKU-1", InventoryPolicy.normalizeSku(" sku-1 "));
        String maxSku = "A".repeat(32);
        assertEquals(maxSku, InventoryPolicy.normalizeSku(maxSku));
    }

    @Test
    void rejectsNullSku() {
        assertThrows(IllegalArgumentException.class, () -> InventoryPolicy.normalizeSku(null));
    }

    @Test
    void rejectsInvalidSku() {
        assertThrows(IllegalArgumentException.class, () -> InventoryPolicy.normalizeSku("-SKU"));
        assertThrows(IllegalArgumentException.class, () -> InventoryPolicy.normalizeSku("품목"));
    }

    @Test
    void derivesStockStatus() {
        assertEquals("OUT_OF_STOCK", InventoryPolicy.stockStatus(0));
        assertEquals("IN_STOCK", InventoryPolicy.stockStatus(1));
        assertEquals("IN_STOCK", InventoryPolicy.stockStatus(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> InventoryPolicy.stockStatus(-1));
    }

    @Test
    void usesJacksonInclusionValueInDomainCode() {
        assertEquals(JsonInclude.Include.NON_NULL, InventoryPolicy.jsonInclusion().getValueInclusion());
        assertEquals(JsonInclude.Include.ALWAYS, InventoryPolicy.jsonInclusion().getContentInclusion());
    }
}
