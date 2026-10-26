// SMP-C21, SMP-C24: host가 타입 참조하고 ServiceLoader로 구현체를 찾는 SPI 계약.
package com.depgov.plugins.spi;

import java.util.Map;

public interface Plugin {
    String id();

    String render(Map<String, String> input);
}
