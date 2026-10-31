package com.depgov.sample.billing;

public record Invoice(String invoiceId, String customer, long quantity, long unitPrice,
                      long subtotal, long tax, long total, String csvLine) {
}
