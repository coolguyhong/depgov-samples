// SMP-C17/SMP-C18: 전이 라이브러리 직접 참조 0. SMP-C19: PartnerClient 호출을 받는 JDK HTTP stub.
// 요청·응답 JSON은 JsonCodec.read/write 정적 호출로 변환한다.
package com.depgov.sample.gateway;

import com.depgov.common.convert.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class PartnerStubServer implements AutoCloseable {
    private final HttpServer server;

    private PartnerStubServer(HttpServer server) {
        this.server = server;
    }

    static PartnerStubServer start(Path fixture) throws IOException {
        StubResponse[] entries = JsonCodec.read(Files.readString(fixture, StandardCharsets.UTF_8),
                StubResponse[].class);
        if (entries == null) {
            throw new IllegalArgumentException("Stub responses must be a JSON array");
        }
        Map<String, StubResponse> responses = new HashMap<>();
        for (StubResponse entry : entries) {
            if (entry == null || entry.path() == null || !entry.path().startsWith("/")
                    || entry.status() < 200 || entry.status() > 599) {
                throw new IllegalArgumentException("Each stub needs an absolute path and status 200..599");
            }
            if (responses.putIfAbsent(entry.path(), entry) != null) {
                throw new IllegalArgumentException("Duplicate stub path: " + entry.path());
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
            StubResponse response = responses.get(path);
            if (!"GET".equals(exchange.getRequestMethod())) {
                reply(exchange, 405, Map.of("message", "Method not allowed"));
            } else if (response == null) {
                reply(exchange, 404, Map.of("message", "No stub response"));
            } else {
                reply(exchange, response.status(), response.body());
            }
        });
        server.start();
        return new PartnerStubServer(server);
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void reply(HttpExchange exchange, int status, Object body) throws IOException {
        try (exchange) {
            byte[] bytes = JsonCodec.write(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            boolean noBody = status == 204 || status == 205 || status == 304;
            exchange.sendResponseHeaders(status, noBody ? -1 : bytes.length);
            if (!noBody) {
                exchange.getResponseBody().write(bytes);
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public record StubResponse(String path, int status, Object body) { }
}
