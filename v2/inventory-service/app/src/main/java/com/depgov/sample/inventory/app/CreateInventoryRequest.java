package com.depgov.sample.inventory.app;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInventoryRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(0) Integer quantity) {
}
