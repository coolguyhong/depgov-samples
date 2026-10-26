// SMP-C10: StringUtils/Validate 정적 호출; SMP-C11: domain 에서만 log4j-api(LogManager) 로깅 — app 은 project(:domain) 경유로만 log4j-api 를 받는다(Claude 보완 2026-10-26: slf4j 는 ext-client 가 외부 경로로도 끌어와 project 전용 경로 사례가 되지 않아 교체).
package com.depgov.sample.order.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class OrderCalculator {
    private static final Logger LOG = LogManager.getLogger(OrderCalculator.class);

    public long subtotal(Order order) {
        Validate.notNull(order, "order is required");
        Validate.isTrue(!StringUtils.isBlank(order.orderId()), "orderId is required");
        Validate.isTrue(!StringUtils.isBlank(order.customerId()), "customerId is required");
        Validate.notEmpty(order.lines(), "at least one order line is required");
        long subtotal = 0;
        for (OrderLine line : order.lines()) {
            Validate.notNull(line, "order line is required");
            Validate.isTrue(!StringUtils.isBlank(line.productId()), "productId is required");
            Validate.isTrue(line.qty() > 0, "qty must be positive");
            Validate.isTrue(line.unitPrice() >= 0, "unitPrice must be nonnegative");
            subtotal = Math.addExact(subtotal, Math.multiplyExact((long) line.qty(), line.unitPrice()));
        }
        LOG.info("Calculated order {} subtotal={}", order.orderId(), subtotal);
        return subtotal;
    }
}
