// SMP-C17/SMP-C18: 직접 참조 없이 loopback 통합 검증. SMP-C19: 실제 PartnerClient 경유.
// SMP-C20: 구현 클래스 참조 없이 runtime binding을 사용한다.
package com.depgov.sample.gateway;

import com.depgov.common.convert.JsonCodec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GatewayAppTest {
    @TempDir
    Path temporary;

    @Test
    void stubCliMatchesExpectedBytesAndContinuesAfter404() throws IOException {
        Path output = temporary.resolve("nested/responses.json");
        GatewayApp.main(new String[]{"--stub", "input/requests.json", output.toString()});

        assertArrayEquals(Files.readAllBytes(Path.of("expected/responses.json")), Files.readAllBytes(output));
        GatewayApp.GatewayResponse[] results = read(output);
        assertEquals(3, results.length);
        assertEquals("한빛상사", results[0].partner().displayName());
        assertTrue(results[0].partner().enabled());
        assertEquals("FAILED", results[1].status());
        assertEquals("Partner GET failed: HTTP 404", results[1].error());
        assertNull(results[1].partner());
        assertEquals("REQ-003", results[2].requestId());
        assertEquals("SUCCESS", results[2].status());
        assertFalse(results[2].partner().enabled());
    }

    @Test
    void baseUrlCliPreservesQueryAndUsesExistingLoopbackServer() throws IOException {
        Path fixture = write("stub-responses.json", """
                [{"path":"/partners?id=P300","status":200,
                  "body":{"id":"P300","companyName":"Query Partner","state":"ACTIVE"}}]
                """);
        Path input = requests("/partners?id=P300");
        Path output = temporary.resolve("responses.json");
        try (PartnerStubServer server = PartnerStubServer.start(fixture)) {
            GatewayApp.main(new String[]{"--base-url", server.baseUrl(), input.toString(), output.toString()});
        }
        assertEquals("SUCCESS", read(output)[0].status());
        assertEquals("P300", read(output)[0].partner().code());
    }

    @Test
    void serverErrorBecomesFailedResult() throws IOException {
        GatewayApp.GatewayResponse result = runOne("""
                [{"path":"/partners/test","status":500,"body":{"message":"Unavailable"}}]
                """);
        assertEquals("FAILED", result.status());
        assertEquals("Partner GET failed: HTTP 500", result.error());
        assertNull(result.partner());
    }

    @Test
    void unknownStubPathBecomes404Result() throws IOException {
        GatewayApp.GatewayResponse result = runOne("[]");
        assertEquals("FAILED", result.status());
        assertEquals("Partner GET failed: HTTP 404", result.error());
    }

    @Test
    void invalidPayloadsBecomeFailedResults() throws IOException {
        for (String body : new String[]{"null", "\"not an object\"",
                "{\"id\":\"P1\",\"companyName\":\"Test\",\"state\":\"UNKNOWN\"}",
                "{\"id\":\"P1\",\"state\":\"ACTIVE\"}"}) {
            GatewayApp.GatewayResponse result = runOne(
                    "[{\"path\":\"/partners/test\",\"status\":200,\"body\":" + body + "}]");
            assertEquals("FAILED", result.status());
            assertEquals("INVALID_PARTNER_RESPONSE", result.error());
            assertNull(result.partner());
        }
    }

    @Test
    void emptyInputWritesEmptyArray() throws IOException {
        write("stub-responses.json", "[]");
        Path input = write("requests.json", "[]");
        Path output = temporary.resolve("responses.json");
        GatewayApp.main(new String[]{"--stub", input.toString(), output.toString()});
        assertEquals("[]\n", Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsExternalBaseUrlBeforeReadingInputOrConnecting() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GatewayApp.main(new String[]{"--base-url", "http://192.0.2.1:8080",
                        temporary.resolve("absent.json").toString(), temporary.resolve("out.json").toString()}));
        assertEquals("Only http://127.0.0.1 loopback URLs are allowed", error.getMessage());
        assertFalse(Files.exists(temporary.resolve("out.json")));
    }

    @Test
    void rejectsInvalidArgumentsAndRequestFilesWithoutWritingOutput() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> GatewayApp.main(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> GatewayApp.main(new String[]{"--stub", "input"}));
        write("stub-responses.json", "[]");
        Path output = temporary.resolve("responses.json");
        for (String json : new String[]{"null", "[null]", "[{\"requestId\":\"R1\",\"path\":\"relative\"}]"}) {
            Path input = write("requests.json", json);
            assertThrows(IllegalArgumentException.class,
                    () -> GatewayApp.main(new String[]{"--stub", input.toString(), output.toString()}));
        }
        assertFalse(Files.exists(output));
    }

    @Test
    void rejectsDuplicateStubPaths() throws IOException {
        Path fixture = write("stub-responses.json", """
                [{"path":"/same","status":200,"body":null},
                 {"path":"/same","status":404,"body":null}]
                """);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PartnerStubServer.start(fixture));
        assertEquals("Duplicate stub path: /same", error.getMessage());
    }

    private GatewayApp.GatewayResponse runOne(String fixture) throws IOException {
        write("stub-responses.json", fixture);
        Path input = requests("/partners/test");
        Path output = temporary.resolve("responses.json");
        GatewayApp.main(new String[]{"--stub", input.toString(), output.toString()});
        GatewayApp.GatewayResponse[] results = read(output);
        assertEquals(1, results.length);
        assertEquals("R1", results[0].requestId());
        return results[0];
    }

    private Path requests(String path) throws IOException {
        return write("requests.json", JsonCodec.write(new GatewayApp.LookupRequest[]{
                new GatewayApp.LookupRequest("R1", path)}));
    }

    private Path write(String filename, String contents) throws IOException {
        return Files.writeString(temporary.resolve(filename), contents, StandardCharsets.UTF_8);
    }

    private GatewayApp.GatewayResponse[] read(Path output) throws IOException {
        return JsonCodec.read(Files.readString(output, StandardCharsets.UTF_8), GatewayApp.GatewayResponse[].class);
    }
}
