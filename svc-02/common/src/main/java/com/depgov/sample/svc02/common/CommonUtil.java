package com.depgov.sample.svc02.common;

import org.apache.commons.lang3.StringUtils;

public final class CommonUtil {

    private CommonUtil() {
    }

    /** commons-text는 선언만 하고, 그 전이(commons-lang3)만 직접 참조한다 → 선언 없는 REFERENCED */
    public static String normalize(String value) {
        return StringUtils.trimToEmpty(value);
    }
}
