// SMP-C07, SMP-C25: 정적 호출; HALF_UP .5 및 long 곱셈 overflow 경계.
package com.depgov.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void calculatesPercentage() {
        assertEquals(120L, Money.roundHalfUp(1000L, 12));
        assertEquals(12L, Money.roundHalfUp(124L, 10));
    }

    @Test
    void roundsPositiveHalfUp() {
        assertEquals(13L, Money.roundHalfUp(125L, 10));
    }

    @Test
    void roundsNegativeHalfAwayFromZero() {
        assertEquals(-13L, Money.roundHalfUp(-125L, 10));
        assertEquals(-13L, Money.roundHalfUp(125L, -10));
    }

    @Test
    void handlesZeroAmountAndRate() {
        assertEquals(0L, Money.roundHalfUp(0L, 100));
        assertEquals(0L, Money.roundHalfUp(123L, 0));
    }

    @Test
    void avoidsIntermediateLongOverflow() {
        assertEquals(Long.MAX_VALUE, Money.roundHalfUp(Long.MAX_VALUE, 100));
        assertEquals(Long.MIN_VALUE, Money.roundHalfUp(Long.MIN_VALUE, 100));
    }

    @Test
    void rejectsAnUnrepresentableResult() {
        assertThrows(ArithmeticException.class, () -> Money.roundHalfUp(Long.MAX_VALUE, 200));
    }
}
