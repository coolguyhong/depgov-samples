// SMP-C21, SMP-C24: 구현체 직접 생성·reflection 없이 ServiceLoader(SPI)만 사용.
package com.depgov.plugins.report;

import com.depgov.plugins.spi.Plugin;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportPluginTest {
    private Plugin loadPlugin() {
        List<Plugin> plugins = ServiceLoader.load(Plugin.class).stream()
                .map(ServiceLoader.Provider::get).toList();
        assertEquals(1, plugins.size());
        return plugins.get(0);
    }

    @Test
    void discoversSingleReportProviderThroughSpi() {
        assertEquals("report", loadPlugin().id());
    }

    @Test
    void sortsLinesByKeyWithoutMutatingInput() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("z", "last");
        input.put("a", "first");
        assertEquals("a=first\nz=last", loadPlugin().render(input));
        assertEquals(List.of("z", "a"), List.copyOf(input.keySet()));
    }

    @Test
    void rendersEmptyInput() {
        assertEquals("", loadPlugin().render(Map.of()));
    }

    @Test
    void preservesEmptyValuesAndUnicode() {
        assertEquals("a=\nb=보고서", loadPlugin().render(Map.of("b", "보고서", "a", "")));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(NullPointerException.class, () -> loadPlugin().render(null));
    }
}
