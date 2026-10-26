// SMP-C10, SMP-C11: domain 계산·검증과 domain 로거 경로를 실행한다.
package com.depgov.sample.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderCalculatorTest {
    private final OrderCalculator calculator = new OrderCalculator();

    @Test
    void sumsQuantityTimesUnitPrice() {
        assertEquals(2999, calculator.subtotal(order(List.of(
                new OrderLine("PEN", 2, 1250), new OrderLine("NOTE", 1, 499)))));
    }

    @Test
    void allowsFreeItems() {
        assertEquals(0, calculator.subtotal(order(List.of(new OrderLine("GIFT", 1, 0)))));
    }

    @Test
    void rejectsBlankIdentifiers() {
        List<OrderLine> lines = List.of(new OrderLine("PEN", 1, 100));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(new Order(" ", "C1", lines, List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(new Order("ORD", " ", lines, List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(order(List.of(new OrderLine(" ", 1, 100)))));
    }

    @Test
    void rejectsEmptyOrder() {
        assertThrows(IllegalArgumentException.class, () -> calculator.subtotal(order(List.of())));
    }

    @Test
    void rejectsNonpositiveQuantityAndNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(order(List.of(new OrderLine("PEN", 0, 100)))));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(order(List.of(new OrderLine("PEN", -1, 100)))));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.subtotal(order(List.of(new OrderLine("PEN", 1, -1)))));
    }

    @Test
    void rejectsMultiplicationAndSumOverflow() {
        assertThrows(ArithmeticException.class,
                () -> calculator.subtotal(order(List.of(new OrderLine("PEN", 2, Long.MAX_VALUE)))));
        assertThrows(ArithmeticException.class,
                () -> calculator.subtotal(order(List.of(
                        new OrderLine("PEN", 1, Long.MAX_VALUE), new OrderLine("NOTE", 1, 1)))));
    }

    private Order order(List<OrderLine> lines) {
        return new Order("ORD", "C1", lines, List.of());
    }
}
