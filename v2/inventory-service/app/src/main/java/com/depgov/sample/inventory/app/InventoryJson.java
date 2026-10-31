package com.depgov.sample.inventory.app;

import com.depgov.sample.inventory.domain.InventoryPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class InventoryJson {
    private final ObjectMapper mapper = new ObjectMapper();

    public InventoryJson() {
        mapper.setDefaultPropertyInclusion(InventoryPolicy.jsonInclusion());
    }

    public String write(InventoryView view) {
        try {
            return mapper.writeValueAsString(view);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot serialize inventory", exception);
        }
    }
}
