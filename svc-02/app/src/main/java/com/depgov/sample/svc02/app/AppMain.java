package com.depgov.sample.svc02.app;

import ch.qos.logback.classic.LoggerContext;
import com.depgov.sample.svc02.core.CoreService;
import org.apache.commons.lang3.StringUtils;

public class AppMain {

    public static void main(String[] args) {
        // starter-logging 자체가 아니라 그 안에 든 logback-classic만 직접 참조한다
        LoggerContext context = new LoggerContext();
        context.setName(StringUtils.defaultIfBlank(args.length > 0 ? args[0] : null, "svc-02"));
        new CoreService().digestName(context.getName());
    }
}
