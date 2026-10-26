// SMP-C09, SMP-C17, SMP-C18, SMP-C19: 서비스 생성·호출 API; HttpClient/Core 직접 참조.
package com.depgov.common.ext;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public final class PartnerClient {
    private final String baseUrl;

    public PartnerClient(String baseUrl) {
        URI uri = URI.create(Objects.requireNonNull(baseUrl, "baseUrl"));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("baseUrl must be an HTTP(S) URL without query or fragment");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String get(String path) {
        Objects.requireNonNull(path, "path");
        HttpGet request = new HttpGet(baseUrl + "/" + path.replaceFirst("^/+", ""));
        try (CloseableHttpClient client = HttpClients.custom().disableRedirectHandling().build()) {
            return client.execute(request, response -> {
                int status = response.getCode();
                if (status < 200 || status >= 300) {
                    throw new PartnerException("Partner GET failed: HTTP " + status);
                }
                return response.getEntity() == null ? ""
                        : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            });
        } catch (IOException e) {
            throw new PartnerException("Partner GET failed: " + request.getRequestUri(), e);
        }
    }
}
