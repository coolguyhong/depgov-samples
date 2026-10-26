// SMP-C07, SMP-C25, SMP-C31, SMP-C32: 서비스 정적 호출용 API; 내부는 StringUtils 정적 호출.
package com.depgov.common.util;

import org.apache.commons.lang3.StringUtils;

public final class StringKit {
    private StringKit() {
    }

    // 1.1.0: pad(String,int) 제거(SMP-C32 변경 영향 사례). padRight 가 같은 의미, padLeft 추가.
    public static String padRight(String s, int width) {
        return StringUtils.rightPad(StringUtils.defaultString(s), width);
    }

    public static String padLeft(String s, int width) {
        return StringUtils.leftPad(StringUtils.defaultString(s), width);
    }

    public static boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }
}
