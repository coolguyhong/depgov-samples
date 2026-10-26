// SMP-C07, SMP-C25: 공통 라이브러리 정적 호출용 금액 계산 API.
package com.depgov.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {
    }

    public static long roundHalfUp(long amount, int ratePercent) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(ratePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
