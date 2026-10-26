// SMP-C07, SMP-C25, SMP-C32: 공개 API 정적 호출의 정상·null·빈 값 경계.
package com.depgov.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringKitTest {
    @Test
    void padsOnTheRight() {
        assertEquals("abc  ", StringKit.pad("abc", 5));
    }

    @Test
    void treatsNullAsEmpty() {
        assertEquals("   ", StringKit.pad(null, 3));
        assertEquals("", StringKit.pad(null, 0));
        assertEquals("  ", StringKit.pad("", 2));
    }

    @Test
    void doesNotTruncateOrPadToNegativeWidths() {
        assertEquals("abc", StringKit.pad("abc", 2));
        assertEquals("abc", StringKit.pad("abc", -1));
    }

    @Test
    void delegatesBlankChecksIncludingNullAndWhitespace() {
        assertTrue(StringKit.isBlank(null));
        assertTrue(StringKit.isBlank(""));
        assertTrue(StringKit.isBlank(" \t\n"));
        assertFalse(StringKit.isBlank(" x "));
    }
}
