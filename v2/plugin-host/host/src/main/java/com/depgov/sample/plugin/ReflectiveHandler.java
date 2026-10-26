// SMP-C22: reflection only; handler class name comes exclusively from YAML.
package com.depgov.sample.plugin;

final class ReflectiveHandler {
    private ReflectiveHandler() {
    }

    static String digest(PluginConfig config) throws ReflectiveOperationException {
        Class<?> handler = Class.forName(config.handler());
        return (String) handler.getMethod("sha256Hex", String.class).invoke(null, config.digestInput());
    }
}
