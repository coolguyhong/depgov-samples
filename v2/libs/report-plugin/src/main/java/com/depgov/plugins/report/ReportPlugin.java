// SMP-C21, SMP-C24: Plugin 구현 provider; host의 사용 방식은 ServiceLoader(SPI) 전용.
package com.depgov.plugins.report;

import com.depgov.plugins.spi.Plugin;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;

public final class ReportPlugin implements Plugin {
    @Override
    public String id() {
        return "report";
    }

    @Override
    public String render(Map<String, String> input) {
        StringJoiner output = new StringJoiner("\n");
        for (Map.Entry<String, String> entry : new TreeMap<>(Objects.requireNonNull(input, "input")).entrySet()) {
            output.add(entry.getKey() + "=" + entry.getValue());
        }
        return output.toString();
    }
}
