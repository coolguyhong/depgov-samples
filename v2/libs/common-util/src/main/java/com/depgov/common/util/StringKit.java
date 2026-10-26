// SMP-C07, SMP-C25, SMP-C31, SMP-C32: 서비스 정적 호출용 API; 내부는 StringUtils 정적 호출.
package com.depgov.common.util;

import org.apache.commons.lang3.StringUtils;

public final class StringKit {
    private StringKit() {
    }

    public static String pad(String s, int width) {
        return StringUtils.rightPad(StringUtils.defaultString(s), width);
    }

    public static boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }
}
