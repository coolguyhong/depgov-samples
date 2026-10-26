// SMP-C23: SnakeYAML constructor and load method are referenced directly.
package com.depgov.sample.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

record PluginConfig(List<String> active, String handler, String handlerMethod, Map<String, String> input) {
    static PluginConfig read(Path path) throws IOException {
        Object document;
        try (InputStream stream = Files.newInputStream(path)) {
            document = new Yaml().load(stream);
        }
        if (!(document instanceof Map<?, ?> config)) {
            throw new IllegalArgumentException("Configuration must be a YAML mapping");
        }
        if (!(config.get("active") instanceof List<?> ids)) {
            throw new IllegalArgumentException("active must be a list of plugin ids");
        }
        List<String> active = new ArrayList<>();
        for (Object id : ids) {
            String name = requiredText(id, "active id");
            if (active.contains(name)) {
                throw new IllegalArgumentException("Duplicate active plugin: " + name);
            }
            active.add(name);
        }
        String handler = requiredText(config.get("handler"), "handler");
        String method = requiredText(config.get("handlerMethod"), "handlerMethod");
        if (!method.equals("sha256Hex")) {
            throw new IllegalArgumentException("handlerMethod must be sha256Hex");
        }
        if (!(config.get("input") instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("input must be a mapping of string keys and values");
        }
        Map<String, String> input = new TreeMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = requiredText(entry.getKey(), "input key");
            if (!(entry.getValue() instanceof String value)) {
                throw new IllegalArgumentException("input values must be strings: " + key);
            }
            input.put(key, value);
        }
        return new PluginConfig(List.copyOf(active), handler, method, Collections.unmodifiableMap(input));
    }

    String digestInput() {
        return input.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String requiredText(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(field + " must be a nonblank string");
        }
        return text;
    }
}
