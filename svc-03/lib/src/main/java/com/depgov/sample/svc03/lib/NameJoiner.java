package com.depgov.sample.svc03.lib;

import com.google.common.base.Joiner;

public class NameJoiner {

    public String join(String[] names) {
        return Joiner.on(", ").skipNulls().join(names);
    }
}
