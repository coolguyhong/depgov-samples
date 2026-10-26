package com.depgov.sample.v2.failmodule.ok;

import org.apache.commons.lang3.StringUtils;

public final class OkReport {
    private OkReport() {
    }

    public static String title(String value) {
        return StringUtils.trimToEmpty(value);
    }
}
