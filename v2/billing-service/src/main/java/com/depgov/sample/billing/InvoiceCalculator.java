package com.depgov.sample.billing;

import com.depgov.common.convert.CsvLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class InvoiceCalculator {
    public Invoice calculate(String invoiceId, InvoiceRequest request) {
        String id = validId(invoiceId);
        if (request == null || StringUtils.isBlank(request.customer())) {
            throw new IllegalArgumentException("customer is required");
        }
        String customer = StringUtils.strip(request.customer());
        if (customer.length() > 120) {
            throw new IllegalArgumentException("customer must be at most 120 characters");
        }
        if (request.quantity() == null || request.quantity() <= 0
                || request.unitPrice() == null || request.unitPrice() < 0) {
            throw new IllegalArgumentException("quantity must be positive and unitPrice nonnegative");
        }
        long subtotal;
        long tax;
        long total;
        try {
            subtotal = Math.multiplyExact(request.quantity(), request.unitPrice());
            tax = BigDecimal.valueOf(subtotal).movePointLeft(1)
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            total = Math.addExact(subtotal, tax);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("invoice amount exceeds the integer range", exception);
        }
        String csv = CsvLine.join(List.of(id, customer, Long.toString(subtotal),
                Long.toString(tax), Long.toString(total)));
        return new Invoice(id, customer, request.quantity(), request.unitPrice(), subtotal, tax, total, csv);
    }

    public String validId(String invoiceId) {
        if (invoiceId == null || !invoiceId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("invoiceId must be 1-64 letters, digits, underscores or hyphens");
        }
        return invoiceId;
    }
}
