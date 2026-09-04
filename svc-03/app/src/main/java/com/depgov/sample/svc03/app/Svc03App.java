package com.depgov.sample.svc03.app;

import com.depgov.sample.svc03.lib.NameJoiner;
import org.apache.commons.lang3.StringUtils;

public class Svc03App {

    public static void main(String[] args) {
        String joined = new NameJoiner().join(args);
        if (StringUtils.isNotBlank(joined)) {
            System.out.println(joined);
        }
    }
}
