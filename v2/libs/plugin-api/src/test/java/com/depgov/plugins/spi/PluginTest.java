// SMP-C24: host와 같은 SPI 타입 참조; 테스트 provider로 정상·빈 입력 계약 확인.
package com.depgov.plugins.spi;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PluginTest {
    private final Plugin plugin = new Plugin() {
        @Override
        public String id() {
            return "test";
        }

        @Override
        public String render(Map<String, String> input) {
            return input.getOrDefault("message", "");
        }
    };

    @Test
    void supportsCallsThroughTheSpiType() {
        assertEquals("test", plugin.id());
        assertEquals("hello", plugin.render(Map.of("message", "hello")));
    }

    @Test
    void supportsEmptyInputAndOutput() {
        assertEquals("", plugin.render(Map.of()));
        assertEquals("", plugin.render(Map.of("message", "")));
    }
}
