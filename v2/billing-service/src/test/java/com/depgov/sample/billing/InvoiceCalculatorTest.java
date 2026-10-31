package com.depgov.sample.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InvoiceCalculatorTest {
    private final InvoiceCalculator calculator = new InvoiceCalculator();

    @Test
    void roundsHalfUpAndTrimsCustomer() {
        Invoice invoice = calculator.calculate("INV-001", new InvoiceRequest("  Acme, Seoul  ", 3L, 335L));
        assertEquals("Acme, Seoul", invoice.customer());
        assertEquals(1005L, invoice.subtotal());
        assertEquals(101L, invoice.tax());
        assertEquals(1106L, invoice.total());
        assertEquals("INV-001,\"Acme, Seoul\",1005,101,1106", invoice.csvLine());
    }

    @Test
    void allowsZeroPrice() {
        Invoice invoice = calculator.calculate("FREE", new InvoiceRequest("Guest", 2L, 0L));
        assertEquals(0L, invoice.subtotal());
        assertEquals(0L, invoice.tax());
        assertEquals(0L, invoice.total());
    }

    @Test
    void roundsTaxBelowHalfDown() {
        Invoice invoice = calculator.calculate("SMALL", new InvoiceRequest("Guest", 1L, 4L));
        assertEquals(0L, invoice.tax());
        assertEquals(4L, invoice.total());
    }

    @Test
    void escapesQuotesInCsvExport() {
        Invoice invoice = calculator.calculate("QUOTE", new InvoiceRequest("A \"team\"", 1L, 5L));
        assertEquals("QUOTE,\"A \"\"team\"\"\",5,1,6", invoice.csvLine());
    }

    @Test
    void rejectsMissingBlankOrLongCustomer() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate("I", null));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest(null, 1L, 1L)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest(" \t ", 1L, 1L)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("a".repeat(121), 1L, 1L)));
    }

    @Test
    void rejectsMissingOrInvalidAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", null, 1L)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", 0L, 1L)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", -1L, 1L)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", 1L, null)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", 1L, -1L)));
    }

    @Test
    void validatesIdLengthAndCharacters() {
        assertEquals("a".repeat(64), calculator.validId("a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> calculator.validId(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.validId(""));
        assertThrows(IllegalArgumentException.class, () -> calculator.validId("bad id"));
        assertThrows(IllegalArgumentException.class, () -> calculator.validId("a".repeat(65)));
    }

    @Test
    void rejectsMultiplicationAndTotalOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", 2L, Long.MAX_VALUE)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("I", new InvoiceRequest("A", 1L, Long.MAX_VALUE)));
    }
}
