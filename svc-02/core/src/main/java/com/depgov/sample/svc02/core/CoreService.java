package com.depgov.sample.svc02.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreService {

    private static final Logger log = LoggerFactory.getLogger(CoreService.class);

    public void digestName(String name) {
        try {
            // commons-codec은 리플렉션으로만 쓴다 — 정적 참조 0건 (HIGH 오탐 케이스)
            Class<?> digestUtils = Class.forName("org.apache.commons.codec.digest.DigestUtils");
            Object hex = digestUtils.getMethod("sha256Hex", String.class).invoke(null, name);
            log.info("digest={}", hex);
        } catch (ReflectiveOperationException e) {
            log.warn("codec 없음", e);
        }
    }
}
