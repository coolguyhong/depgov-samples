// SMP-C07, SMP-C25, SMP-C32: 공개 API 정적 호출의 정상·null·빈 값 경계.
package com.depgov.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringKitTest {
    @Test
    void padsOnTheLeft() {
        assertEquals("  abc", StringKit.padLeft("abc", 5));
        assertEquals("   ", StringKit.padLeft(null, 3));
    }

    @org.junit.jupiter.api.Test
    void padsOnTheRight() {
        assertEquals("abc  ", StringKit.padRight("abc", 5));
    }

    @Test
    void treatsNullAsEmpty() {
        assertEquals("   ", StringKit.padRight(null, 3));
        assertEquals("", StringKit.padRight(null, 0));
        assertEquals("  ", StringKit.padRight("", 2));
    }

    @Test
    void doesNotTruncateOrPadToNegativeWidths() {
        assertEquals("abc", StringKit.padRight("abc", 2));
        assertEquals("abc", StringKit.padRight("abc", -1));
    }

    @Test
    void delegatesBlankChecksIncludingNullAndWhitespace() {
        assertTrue(StringKit.isBlank(null));
        assertTrue(StringKit.isBlank(""));
        assertTrue(StringKit.isBlank(" \t\n"));
        assertFalse(StringKit.isBlank(" x "));
    }
}
