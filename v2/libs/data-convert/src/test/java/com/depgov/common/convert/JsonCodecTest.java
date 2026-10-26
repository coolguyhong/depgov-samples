// SMP-C08, SMP-C12, SMP-C33: 정적 JSON 호출; 정상·null·잘못된 입력 경계.
package com.depgov.common.convert;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonCodecTest {
    public static class Order {
        public String id;
        public int amount;
    }

    @Test
    void readsAndWritesTypedObjects() {
        Order order = JsonCodec.read("{\"id\":\"order-1\",\"amount\":125}", Order.class);
        assertEquals("order-1", order.id);
        assertEquals(125, order.amount);
        Order copy = JsonCodec.read(JsonCodec.write(order), Order.class);
        assertEquals(order.id, copy.id);
        assertEquals(order.amount, copy.amount);
    }

    @Test
    void serializesNullAndEmptyValues() {
        assertEquals("null", JsonCodec.write(null));
        assertNull(JsonCodec.read("null", Order.class));
        assertEquals("{}", JsonCodec.write(Map.of()));
        assertEquals("", JsonCodec.read("\"\"", String.class));
    }

    @Test
    void rejectsNullEmptyAndMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> JsonCodec.read(null, Order.class));
        assertThrows(IllegalArgumentException.class, () -> JsonCodec.read("", Order.class));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.read("{", Order.class));
        assertNotNull(error.getCause());
    }

    @Test
    void wrapsSerializationFailure() {
        OrderWithCycle cycle = new OrderWithCycle();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.write(cycle));
        assertNotNull(error.getCause());
    }

    public static class OrderWithCycle {
        public OrderWithCycle self = this;
    }
}
