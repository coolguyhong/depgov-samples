// SMP-C17/SMP-C18: 전이 의존, 직접 참조 0. SMP-C19: PartnerClient 생성·get 호출·예외 catch.
// SMP-C20: runtime binding 전용, Logger/LoggerFactory만 정적 참조. JsonCodec.read/write 정적 호출.
package com.depgov.sample.gateway;

import com.depgov.common.convert.JsonCodec;
import com.depgov.common.ext.PartnerClient;
import com.depgov.common.ext.PartnerException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GatewayApp {
    private static final Logger LOG = LoggerFactory.getLogger(GatewayApp.class);

    private GatewayApp() {
    }

    public static void main(String[] args) throws IOException {
        Options options = Options.parse(args);
        if (options.stub()) {
            Path stubFile = options.input().toAbsolutePath().resolveSibling("stub-responses.json");
            try (PartnerStubServer server = PartnerStubServer.start(stubFile)) {
                run(server.baseUrl(), options.input(), options.output());
            }
        } else {
            run(options.baseUrl(), options.input(), options.output());
        }
    }

    static void run(String baseUrl, Path input, Path output) throws IOException {
        requireLoopback(baseUrl);
        LookupRequest[] requests = JsonCodec.read(Files.readString(input, StandardCharsets.UTF_8),
                LookupRequest[].class);
        if (requests == null) {
            throw new IllegalArgumentException("Requests must be a JSON array");
        }
        for (LookupRequest request : requests) {
            if (request == null || request.requestId() == null || request.requestId().isBlank()
                    || request.path() == null || !request.path().startsWith("/")) {
                throw new IllegalArgumentException("Each request needs requestId and an absolute path");
            }
        }

        PartnerClient client = new PartnerClient(baseUrl);
        List<GatewayResponse> responses = new ArrayList<>();
        int failed = 0;
        for (LookupRequest request : requests) {
            GatewayResponse response;
            try {
                String json = client.get(request.path());
                PartnerPayload payload = JsonCodec.read(json, PartnerPayload.class);
                if (payload == null || payload.id() == null || payload.id().isBlank()
                        || payload.companyName() == null || payload.companyName().isBlank()
                        || !("ACTIVE".equals(payload.state()) || "INACTIVE".equals(payload.state()))) {
                    throw new IllegalArgumentException("Invalid partner payload");
                }
                PartnerData partner = new PartnerData(payload.id(), payload.companyName(),
                        "ACTIVE".equals(payload.state()));
                response = new GatewayResponse(request.requestId(), "SUCCESS", partner, null);
                LOG.info("request={} status=SUCCESS", request.requestId());
            } catch (PartnerException e) {
                response = new GatewayResponse(request.requestId(), "FAILED", null, e.getMessage());
                LOG.warn("request={} status=FAILED reason={}", request.requestId(), e.getMessage());
                failed++;
            } catch (IllegalArgumentException e) {
                response = new GatewayResponse(request.requestId(), "FAILED", null, "INVALID_PARTNER_RESPONSE");
                LOG.warn("request={} status=FAILED reason=INVALID_PARTNER_RESPONSE", request.requestId());
                failed++;
            }
            responses.add(response);
        }

        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, JsonCodec.write(responses) + "\n", StandardCharsets.UTF_8);
        LOG.info("Processed {} requests: success={}, failed={}; output={}",
                requests.length, requests.length - failed, failed, output);
    }

    private static void requireLoopback(String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (!"http".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost())
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Only http://127.0.0.1 loopback URLs are allowed");
        }
    }

    public record LookupRequest(String requestId, String path) { }

    public record PartnerPayload(String id, String companyName, String state) { }

    public record PartnerData(String code, String displayName, boolean enabled) { }

    public record GatewayResponse(String requestId, String status, PartnerData partner, String error) { }

    private record Options(boolean stub, String baseUrl, Path input, Path output) {
        private static Options parse(String[] args) {
            boolean stub = args.length > 0 && "--stub".equals(args[0]);
            int offset = stub ? 1 : 2;
            if ((!stub && (args.length < 2 || !"--base-url".equals(args[0])))
                    || (args.length != offset && args.length != offset + 2)) {
                throw new IllegalArgumentException("Usage: --stub [input output] | --base-url URL [input output]");
            }
            Path input = Path.of(args.length == offset ? "input/requests.json" : args[offset]);
            Path output = Path.of(args.length == offset ? "output/responses.json" : args[offset + 1]);
            return new Options(stub, stub ? null : args[1], input, output);
        }
    }
}
