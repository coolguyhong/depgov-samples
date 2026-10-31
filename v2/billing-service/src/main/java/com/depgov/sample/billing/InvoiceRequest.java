package com.depgov.sample.billing;

public record InvoiceRequest(String customer, Long quantity, Long unitPrice) {
}
