// SMP-C21: discover providers only through ServiceLoader; SMP-C24: Plugin type and interface calls.
package com.depgov.sample.plugin;

import com.depgov.plugins.spi.Plugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class HostApp {
    private HostApp() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("Usage: HostApp [input/plugins.yml output/report.txt]");
        }
        Path input = Path.of(args.length == 0 ? "input/plugins.yml" : args[0]);
        Path output = Path.of(args.length == 0 ? "output/report.txt" : args[1]);
        String report = render(PluginConfig.read(input));
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, report, StandardCharsets.UTF_8);
        System.out.print(report);
    }

    static String render(PluginConfig config) throws ReflectiveOperationException {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        Map<String, Plugin> plugins = new LinkedHashMap<>();
        for (Plugin plugin : loader) {
            if (plugins.putIfAbsent(plugin.id(), plugin) != null) {
                throw new IllegalStateException("Duplicate plugin id: " + plugin.id());
            }
        }
        StringBuilder report = new StringBuilder();
        for (String id : config.active()) {
            Plugin plugin = plugins.get(id);
            if (plugin == null) {
                throw new IllegalArgumentException("Active plugin not found: " + id);
            }
            String rendered = plugin.render(config.input());
            if (!rendered.isEmpty()) {
                report.append(rendered).append('\n');
            }
        }
        return report.append("digest=").append(ReflectiveHandler.digest(config)).append('\n').toString();
    }
}
