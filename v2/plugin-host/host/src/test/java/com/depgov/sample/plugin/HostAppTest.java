// SMP-C21/C24: SPI discovery and interface calls only; SMP-C22: reflection path verified against JDK SHA-256.
// SMP-C23: real YAML fixture is parsed through PluginConfig.
package com.depgov.sample.plugin;

import com.depgov.plugins.spi.Plugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HostAppTest {
    @TempDir
    Path temporary;

    @Test
    void discoversAndExecutesReportThroughServiceLoader() throws Exception {
        Plugin plugin = ServiceLoader.load(Plugin.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(candidate -> candidate.id().equals("report"))
                .findFirst().orElseThrow();
        assertEquals("amount=12500\ncustomer=C-1001", plugin.render(fixture().input()));
    }

    @Test
    void reflectiveHandlerComputesRealDigestFromSortedInput() throws Exception {
        PluginConfig config = fixture();
        assertEquals("amount=12500\ncustomer=C-1001", config.digestInput());
        assertEquals(sha256("amount=12500\ncustomer=C-1001"), ReflectiveHandler.digest(config));
    }

    @Test
    void writesExactExpectedReportAndCreatesOutputDirectories() throws Exception {
        Path output = temporary.resolve("nested/report.txt");
        HostApp.main(new String[] {"input/plugins.yml", output.toString()});
        assertEquals(-1L, Files.mismatch(Path.of("expected/report.txt"), output));
    }

    @Test
    void executesOnlyActivePlugins() throws Exception {
        PluginConfig config = changedFixture("active: [report]", "active: []");
        assertEquals("digest=" + sha256("amount=12500\ncustomer=C-1001") + "\n", HostApp.render(config));
    }

    @Test
    void changedInputChangesReflectionDigest() throws Exception {
        PluginConfig config = changedFixture("C-1001", "고객-2002");
        assertEquals(sha256("amount=12500\ncustomer=고객-2002"), ReflectiveHandler.digest(config));
        assertNotEquals(ReflectiveHandler.digest(fixture()), ReflectiveHandler.digest(config));
    }

    @Test
    void missingActivePluginFails() throws Exception {
        PluginConfig config = changedFixture("active: [report]", "active: [missing]");
        assertEquals("Active plugin not found: missing",
                assertThrows(IllegalArgumentException.class, () -> HostApp.render(config)).getMessage());
    }

    @Test
    void missingHandlerClassFailsInsteadOfProducingFallbackDigest() throws Exception {
        String yaml = Files.readString(Path.of("input/plugins.yml"))
                .replaceFirst("(?m)^handler: .+$", "handler: missing.Handler");
        Path input = temporary.resolve("missing-handler.yml");
        Files.writeString(input, yaml);
        PluginConfig config = PluginConfig.read(input);
        assertThrows(ClassNotFoundException.class, () -> HostApp.render(config));
    }

    @Test
    void rejectsUnsupportedHandlerMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> changedFixture("handlerMethod: sha256Hex", "handlerMethod: unknown"));
    }

    @Test
    void rejectsNonStringInputValues() {
        assertThrows(IllegalArgumentException.class, () -> changedFixture("\"12500\"", "12500"));
    }

    @Test
    void rejectsDuplicateActiveIds() {
        assertThrows(IllegalArgumentException.class,
                () -> changedFixture("active: [report]", "active: [report, report]"));
    }

    private PluginConfig fixture() throws Exception {
        return PluginConfig.read(Path.of("input/plugins.yml"));
    }

    private PluginConfig changedFixture(String before, String after) throws Exception {
        Path input = temporary.resolve("plugins.yml");
        Files.writeString(input, Files.readString(Path.of("input/plugins.yml")).replace(before, after));
        return PluginConfig.read(input);
    }

    private static String sha256(String input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8)));
    }
}
