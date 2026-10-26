// SMP-C12: transitive ObjectMapper.readTree direct typed call; no direct dependency declaration.
// SMP-C13: StringUtils static calls; declared 3.16.0, selected 3.17.0.
// SMP-C14, SMP-C15: declaration only; no source references or runtime loading.
// SMP-C16: FileUtils.readLines / writeStringToFile static calls.
// Common APIs: CsvLine.split, JsonCodec.write, Money.roundHalfUp, StringKit.padRight static calls (v2.2: common-util 1.1.0).
package com.depgov.sample.settlement;

import com.depgov.common.convert.CsvLine;
import com.depgov.common.convert.JsonCodec;
import com.depgov.common.util.Money;
import com.depgov.common.util.StringKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class SettlementBatch {
    private static final List<String> HEADER = List.of(
            "merchantId", "transactionId", "amount", "feeRatePercent", "status");
    private static final Set<String> STATUSES = Set.of("APPROVED", "CANCELLED", "DECLINED");

    private SettlementBatch() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 0 && args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: SettlementBatch [transactions.csv config.json settlement.json]");
        }
        Path input = Path.of(args.length == 0 ? "input/transactions.csv" : args[0]);
        Path config = Path.of(args.length == 0 ? "input/config.json" : args[1]);
        Path output = Path.of(args.length == 0 ? "output/settlement.json" : args[2]);
        System.out.println(summary(run(input, config, output)));
    }

    public static SettlementResult run(Path input, Path config, Path output) throws IOException {
        int maxFeeRate = readMaxFeeRate(config);
        List<String> lines = FileUtils.readLines(input.toFile(), StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(CsvLine.split(lines.get(0)))) {
            throw new IllegalArgumentException("CSV header must be " + String.join(",", HEADER));
        }

        Map<String, Totals> merchants = new TreeMap<>();
        Set<String> transactionIds = new HashSet<>();
        Totals total = new Totals();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            Transaction transaction = parseTransaction(line, index + 1);
            if (!transactionIds.add(transaction.transactionId())) {
                throw new IllegalArgumentException("Duplicate transactionId at CSV line " + (index + 1));
            }
            if (!"APPROVED".equals(transaction.status())) {
                continue;
            }
            long fee = Money.roundHalfUp(transaction.amount(), Math.min(transaction.feeRate(), maxFeeRate));
            merchants.computeIfAbsent(transaction.merchantId(), key -> new Totals())
                    .add(transaction.amount(), fee);
            total.add(transaction.amount(), fee);
        }

        List<MerchantSettlement> settlements = new ArrayList<>();
        for (Map.Entry<String, Totals> entry : merchants.entrySet()) {
            Totals value = entry.getValue();
            settlements.add(new MerchantSettlement(entry.getKey(), value.count,
                    value.gross, value.fees, value.gross - value.fees));
        }
        SettlementResult result = new SettlementResult(List.copyOf(settlements), total.count,
                total.gross, total.fees, total.gross - total.fees);
        FileUtils.writeStringToFile(output.toFile(), JsonCodec.write(result) + "\n", StandardCharsets.UTF_8);
        return result;
    }

    public static String summary(SettlementResult result) {
        return StringKit.padRight("SETTLEMENT", 12)
                + "merchants=" + result.merchants().size()
                + " approved=" + result.approvedTransactions()
                + " gross=" + result.grossAmount()
                + " fees=" + result.feeAmount()
                + " net=" + result.netAmount();
    }

    private static int readMaxFeeRate(Path config) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(config.toFile());
        JsonNode rate = root == null ? null : root.get("maxFeeRatePercent");
        if (rate == null || !rate.isIntegralNumber() || !rate.canConvertToInt()
                || rate.intValue() < 0 || rate.intValue() > 100) {
            throw new IllegalArgumentException("maxFeeRatePercent must be an integer from 0 to 100");
        }
        return rate.intValue();
    }

    private static Transaction parseTransaction(String line, int lineNumber) {
        try {
            List<String> fields = CsvLine.split(line);
            if (fields.size() != HEADER.size()) {
                throw new IllegalArgumentException("Expected five CSV fields");
            }
            String merchant = fields.get(0);
            String id = fields.get(1);
            if (StringUtils.isBlank(merchant) || StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("merchantId and transactionId must not be blank");
            }
            long amount = Long.parseLong(fields.get(2));
            int rate = Integer.parseInt(fields.get(3));
            String status = fields.get(4);
            if (amount < 0 || rate < 0 || rate > 100 || !STATUSES.contains(status)) {
                throw new IllegalArgumentException("Invalid amount, feeRatePercent or status");
            }
            return new Transaction(merchant, id, amount, rate, status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid CSV line " + lineNumber + ": " + e.getMessage(), e);
        }
    }

    public record MerchantSettlement(String merchantId, long approvedTransactions,
                                     long grossAmount, long feeAmount, long netAmount) {
    }

    public record SettlementResult(List<MerchantSettlement> merchants, long approvedTransactions,
                                   long grossAmount, long feeAmount, long netAmount) {
    }

    private record Transaction(String merchantId, String transactionId, long amount, int feeRate, String status) {
    }

    private static final class Totals {
        private long count;
        private long gross;
        private long fees;

        private void add(long amount, long fee) {
            count = Math.addExact(count, 1);
            gross = Math.addExact(gross, amount);
            fees = Math.addExact(fees, fee);
        }
    }
}
