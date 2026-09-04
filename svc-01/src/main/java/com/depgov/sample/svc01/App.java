package com.depgov.sample.svc01;

import com.fasterxml.jackson.core.JsonFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "sample.txt";
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("file name is blank");
        }
        // jackson-core(전이 의존성)만 직접 참조한다. databind 클래스는 쓰지 않는다.
        JsonFactory factory = new JsonFactory();
        log.info("ext={} format={}", FilenameUtils.getExtension(name), factory.getFormatName());
    }
}

// 컴파일 오류 주입 (AVS-04)
class Broken { void x() { int y = "not-an-int" } }
