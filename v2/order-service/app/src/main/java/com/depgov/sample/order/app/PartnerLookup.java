// SMP-C05: Nullable은 파라미터 annotation 전용; SMP-C09: 옵션이 있을 때만 PartnerClient 생성·get 호출.
package com.depgov.sample.order.app;

import com.depgov.common.ext.PartnerClient;
import javax.annotation.Nullable;

public final class PartnerLookup {
    public String lookup(@Nullable String baseUrl, String id) {
        if (baseUrl == null) {
            return null;
        }
        return new PartnerClient(baseUrl).get("/partners/" + id);
    }
}
