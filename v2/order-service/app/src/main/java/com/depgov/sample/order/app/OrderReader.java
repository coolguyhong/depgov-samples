// SMP-C02: new ObjectMapper() 및 readValue 메서드로 입력 JSON을 주문 타입으로 읽기.
package com.depgov.sample.order.app;

import com.depgov.sample.order.domain.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class OrderReader {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Order> read(Path input) throws IOException {
        OrderInput document = mapper.readValue(input.toFile(), OrderInput.class);
        if (document == null || document.orders() == null || document.orders().isEmpty()) {
            throw new IllegalArgumentException("input must contain at least one order");
        }
        return List.copyOf(document.orders());
    }

    public record OrderInput(List<Order> orders) {
    }
}
