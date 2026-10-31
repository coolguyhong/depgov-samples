package com.depgov.sample.inventory.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public final class InventoryPolicy {
    private InventoryPolicy() {
    }

    public static String normalizeSku(String rawSku) {
        String sku = StringUtils.trimToEmpty(rawSku).toUpperCase(Locale.ROOT);
        if (!sku.matches("[A-Z0-9][A-Z0-9-]{0,31}")) {
            throw new IllegalArgumentException("sku must contain 1-32 ASCII letters, digits or hyphens");
        }
        return sku;
    }

    public static String stockStatus(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        return quantity == 0 ? "OUT_OF_STOCK" : "IN_STOCK";
    }

    public static JsonInclude.Value jsonInclusion() {
        return JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS);
    }
}
