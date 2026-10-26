// SMP-C10: 수량과 정수 단가를 담는 OrderCalculator 입력 타입.
package com.depgov.sample.order.domain;

public record OrderLine(String productId, int qty, long unitPrice) {
}
