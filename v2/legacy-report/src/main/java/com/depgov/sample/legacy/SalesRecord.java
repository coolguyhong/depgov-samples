// Fixed-width input for SMP-C25: Java 8 date parsing and integer sales amounts.
package com.depgov.sample.legacy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class SalesRecord {
    private final YearMonth month;
    private final long amount;

    private SalesRecord(YearMonth month, long amount) {
        this.month = month;
        this.amount = amount;
    }

    static SalesRecord parse(String line, int lineNumber) {
        if (!line.matches("[0-9]{8}[A-Z0-9]{1,6} *[0-9]{12}") || line.length() != 26) {
            throw new IllegalArgumentException("Invalid fixed-width sale at line " + lineNumber);
        }
        try {
            LocalDate date = LocalDate.parse(line.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
            long amount = Long.parseLong(line.substring(14, 26));
            return new SalesRecord(YearMonth.from(date), amount);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid sale date at line " + lineNumber, ex);
        }
    }

    YearMonth month() {
        return month;
    }

    long amount() {
        return amount;
    }
}
