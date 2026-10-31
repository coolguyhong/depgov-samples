package com.depgov.sample.notify;

import com.depgov.common.ext.PartnerClient;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpPost;

public final class DeliveryPlanner {
    public String deliveryUrl(String baseUrl) {
        // 사내 클라이언트의 생성자 검증을 재사용한다. get()이나 네트워크 전송은 수행하지 않는다.
        new PartnerClient(baseUrl);
        URI base = URI.create(baseUrl);
        if (base.getRawUserInfo() != null) {
            throw new IllegalArgumentException("partnerBaseUrl must not contain user info");
        }
        HttpPost request = new HttpPost(baseUrl.replaceAll("/+$", "") + "/notifications");
        try {
            return request.getUri().toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid delivery URI", e);
        }
    }
}
