// SMP-C25: StringKit.padRight(1.1.0; 1.0.0 에서는 pad) / Money.roundHalfUp static calls (Java 8 common-util).
// SMP-C26: LoggerFactory.getLogger static call; SLF4J 1.7.36 stays selected.
// SMP-C27: LogManager.getLogger static call; one separate audit event per report.
// SMP-C28: declaration only in build.gradle; no source or bytecode reference.
package com.depgov.sample.legacy;

import com.depgov.common.util.Money;
import com.depgov.common.util.StringKit;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;

public final class LegacyReport {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyReport.class);
    private static final org.apache.logging.log4j.Logger AUDIT = LogManager.getLogger("legacy.audit");

    private LegacyReport() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("Usage: LegacyReport [input/sales.txt output/report.txt]");
        }
        Path input = Paths.get(args.length == 0 ? "input/sales.txt" : args[0]);
        Path output = Paths.get(args.length == 0 ? "output/report.txt" : args[1]);
        generate(input, output);
        System.out.println("Report written: " + output);
    }

    public static void generate(Path input, Path output) throws IOException {
        Map<YearMonth, MonthlySales> months = new TreeMap<YearMonth, MonthlySales>();
        LOG.info("Reading sales from {}", input);
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                SalesRecord record = SalesRecord.parse(line, ++lineNumber);
                MonthlySales sales = months.get(record.month());
                if (sales == null) {
                    sales = new MonthlySales();
                    months.put(record.month(), sales);
                }
                sales.count = Math.addExact(sales.count, 1L);
                sales.amount = Math.addExact(sales.amount, record.amount());
            }
        }

        StringBuilder report = new StringBuilder("MONTHLY SALES REPORT (KRW)\n");
        report.append(StringKit.padRight("MONTH", 7)).append(' ')
                .append(StringUtils.rightPad("RECORDS", 7)).append(' ')
                .append(StringUtils.leftPad("SALES", 12)).append(' ')
                .append(StringUtils.leftPad("TAX", 12)).append(' ')
                .append(StringUtils.leftPad("TOTAL", 12)).append('\n');
        long count = 0;
        long amount = 0;
        long tax = 0;
        for (Map.Entry<YearMonth, MonthlySales> entry : months.entrySet()) {
            MonthlySales sales = entry.getValue();
            long monthlyTax = Money.roundHalfUp(sales.amount, 10);
            appendRow(report, entry.getKey().toString(), sales.count, sales.amount, monthlyTax);
            count = Math.addExact(count, sales.count);
            amount = Math.addExact(amount, sales.amount);
            tax = Math.addExact(tax, monthlyTax);
        }
        appendRow(report, "TOTAL", count, amount, tax);
        Path parent = output.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.write(output, report.toString().getBytes(StandardCharsets.UTF_8));
        AUDIT.info("REPORT_CREATED records={} months={} sales={} output={}",
                count, months.size(), amount, output);
        LOG.info("Completed report with {} records", count);
    }

    private static void appendRow(StringBuilder report, String month, long count, long amount, long tax) {
        report.append(StringKit.padRight(month, 7)).append(' ')
                .append(StringUtils.leftPad(Long.toString(count), 7)).append(' ')
                .append(StringUtils.leftPad(Long.toString(amount), 12)).append(' ')
                .append(StringUtils.leftPad(Long.toString(tax), 12)).append(' ')
                .append(StringUtils.leftPad(Long.toString(Math.addExact(amount, tax)), 12)).append('\n');
    }

    private static final class MonthlySales {
        private long count;
        private long amount;
    }
}
