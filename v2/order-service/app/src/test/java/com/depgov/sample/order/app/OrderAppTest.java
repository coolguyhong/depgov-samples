// SMP-C01~C03, SMP-C05~C09: main 시나리오·JSON 동등성·로컬 파트너 GET 1회를 검증.
package com.depgov.sample.order.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrderAppTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path sampleRoot = Path.of(System.getProperty("sample.root"));

    @TempDir
    Path temporary;

    @Test
    void sampleMatchesExpectedAndStreamsOneSummaryLine() throws Exception {
        Path output = temporary.resolve("nested/summary.json");
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream stdout = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(stdout);
            OrderApp.main(new String[]{sampleRoot.resolve("input/orders.json").toString(), output.toString()});
        } finally {
            System.setOut(original);
        }
        assertEquals(mapper.readTree(sampleRoot.resolve("expected/summary.json").toFile()),
                mapper.readTree(output.toFile()));
        assertEquals("{\"orderCount\":3,\"subtotal\":16801,\"tax\":1681,\"total\":18482}"
                + System.lineSeparator(), captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    void partnerOptionMakesExactlyOneLocalGet() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/partners/", exchange -> {
            calls.incrementAndGet();
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            byte[] body = "{\"name\":\"Local Partner\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) {
                response.write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            Path input = temporary.resolve("one-order.json");
            Files.writeString(input, """
                    {"orders":[{"orderId":"LOCAL-1","customerId":"C1",
                      "lines":[{"productId":"P1","qty":1,"unitPrice":5}]}]}
                    """);
            Path output = temporary.resolve("partner-summary.json");
            OrderApp.main(new String[]{input.toString(), output.toString(), "--partner-url",
                    "http://127.0.0.1:" + server.getAddress().getPort()});
            JsonNode result = mapper.readTree(output.toFile());
            assertEquals(1, calls.get());
            assertEquals("GET", method.get());
            assertEquals("/partners/C1", path.get());
            assertEquals("{\"name\":\"Local Partner\"}",
                    result.path("orders").get(0).path("partnerResponse").asText());
            assertEquals("C1      ", result.path("orders").get(0).path("customerId").asText());
            assertEquals(1, result.path("tax").asLong());
            assertEquals(6, result.path("total").asLong());
            assertEquals(0, result.path("orders").get(0).path("attachmentExtensions").size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void missingPartnerOptionSkipsClient() {
        assertNull(new PartnerLookup().lookup(null, "C1"));
    }

    @Test
    void rejectsInvalidArgumentsBeforeCreatingOutput() {
        Path output = temporary.resolve("invalid.json");
        assertThrows(IllegalArgumentException.class, () -> OrderApp.main(new String[]{}));
        assertThrows(IllegalArgumentException.class,
                () -> OrderApp.main(new String[]{" ", output.toString()}));
        assertThrows(IllegalArgumentException.class,
                () -> OrderApp.main(new String[]{"input.json", output.toString(), "--other", "value"}));
        assertThrows(IllegalArgumentException.class,
                () -> OrderApp.main(new String[]{"input.json", output.toString(), "--partner-url", " "}));
        assertFalse(Files.exists(output));
    }

    @Test
    void rejectsEmptyInputBeforeCreatingOutput() throws Exception {
        Path input = temporary.resolve("empty.json");
        Path output = temporary.resolve("empty-summary.json");
        Files.writeString(input, "{\"orders\":[]}");
        assertThrows(IllegalArgumentException.class,
                () -> OrderApp.main(new String[]{input.toString(), output.toString()}));
        assertFalse(Files.exists(output));
    }
}
