// SMP-C09, SMP-C17, SMP-C18, SMP-C19: 생성·호출; 127.0.0.1 임의 포트 HTTP만 사용.
package com.depgov.common.ext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartnerClientTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ok", exchange ->
                reply(exchange, 200, exchange.getRequestMethod() + ":응답:" + exchange.getRequestURI().getRawQuery()));
        server.createContext("/api/created", exchange -> reply(exchange, 201, "created"));
        server.createContext("/api/empty", exchange -> reply(exchange, 204, ""));
        server.createContext("/api/missing", exchange -> reply(exchange, 404, "missing"));
        server.createContext("/api/error", exchange -> reply(exchange, 500, "error"));
        server.createContext("/api/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/api/ok");
            reply(exchange, 302, "redirect");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void reply(HttpExchange exchange, int status, String text) throws IOException {
        try (exchange) {
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : body.length);
            if (status != 204) {
                exchange.getResponseBody().write(body);
            }
        }
    }

    @Test
    void getsUtf8BodyAndPreservesBasePathAndQuery() {
        assertEquals("GET:응답:id=1", new PartnerClient(baseUrl).get("ok?id=1"));
    }

    @Test
    void normalizesBoundarySlashes() {
        assertEquals("GET:응답:null", new PartnerClient(baseUrl + "/").get("/ok"));
    }

    @Test
    void acceptsOtherSuccessfulStatuses() {
        assertEquals("created", new PartnerClient(baseUrl).get("created"));
    }

    @Test
    void returnsEmptyBodyForNoContent() {
        assertEquals("", new PartnerClient(baseUrl).get("empty"));
    }

    @Test
    void rejectsNotFound() {
        PartnerException error = assertThrows(PartnerException.class,
                () -> new PartnerClient(baseUrl).get("missing"));
        assertTrue(error.getMessage().contains("404"));
    }

    @Test
    void rejectsServerErrors() {
        PartnerException error = assertThrows(PartnerException.class,
                () -> new PartnerClient(baseUrl).get("error"));
        assertTrue(error.getMessage().contains("500"));
    }

    @Test
    void rejectsRedirectsWithoutFollowingThem() {
        PartnerException error = assertThrows(PartnerException.class,
                () -> new PartnerClient(baseUrl).get("redirect"));
        assertTrue(error.getMessage().contains("302"));
    }

    @Test
    void rejectsInvalidAndNullArguments() {
        assertThrows(NullPointerException.class, () -> new PartnerClient(null));
        assertThrows(IllegalArgumentException.class, () -> new PartnerClient(""));
        assertThrows(IllegalArgumentException.class, () -> new PartnerClient("file:///tmp"));
        assertThrows(IllegalArgumentException.class, () -> new PartnerClient(baseUrl + "?q=x"));
        assertThrows(NullPointerException.class, () -> new PartnerClient(baseUrl).get(null));
    }
}
