// SMP-C10: OrderCalculator가 검증하고 계산하는 주문 입력 타입.
package com.depgov.sample.order.domain;

import java.util.List;

public record Order(String orderId, String customerId, List<OrderLine> lines, List<String> attachments) {
}
