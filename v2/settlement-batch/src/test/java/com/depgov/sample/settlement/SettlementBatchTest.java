package com.depgov.sample.settlement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementBatchTest {
    private static final String HEADER = "merchantId,transactionId,amount,feeRatePercent,status\n";

    @TempDir
    Path temp;

    @Test
    void mainWritesExactGoldenFileAndOneLineSummary() throws IOException {
        Path output = temp.resolve("nested/settlement.json");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try (PrintStream capture = new PrintStream(stdout, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            SettlementBatch.main(new String[]{"input/transactions.csv", "input/config.json", output.toString()});
        } finally {
            System.setOut(previous);
        }
        assertEquals(Files.readString(Path.of("expected/settlement.json")), Files.readString(output));
        assertEquals("SETTLEMENT  merchants=3 approved=6 gross=5033 fees=105 net=4928"
                + System.lineSeparator(), stdout.toString(StandardCharsets.UTF_8));
    }

    @Test
    void roundsHalfUpForEachTransactionBeforeSummingAndCapsTheRate() throws IOException {
        var result = run("A,T1,50,1,APPROVED\nA,T2,50,1,APPROVED\nA,T3,1050,10,APPROVED\n", 3);
        assertEquals(3, result.approvedTransactions());
        assertEquals(1150, result.grossAmount());
        assertEquals(34, result.feeAmount()); // 1 + 1 + 32; aggregate rounding would differ
        assertEquals(1116, result.netAmount());
    }

    @Test
    void excludesNonApprovedMerchantsAndKeepsQuotedUnicodeNamesSorted() throws IOException {
        var result = run("Z,T1,100,1,DECLINED\nZ,T2,100,1,CANCELLED\n"
                + "\"한글,상점\",T3,100,1,APPROVED\n\"A\"\"샵\",T4,200,2,APPROVED\n\n", 3);
        assertEquals(List.of("A\"샵", "한글,상점"), List.of(
                result.merchants().get(0).merchantId(), result.merchants().get(1).merchantId()));
        assertEquals(2, result.approvedTransactions());
        assertEquals(300, result.grossAmount());
        assertEquals(5, result.feeAmount());
        assertEquals(295, result.netAmount());
    }

    @Test
    void supportsZeroAndFullFeeCaps() throws IOException {
        var zero = run("A,T1,123,100,APPROVED\nA,T2,0,10,APPROVED\n", 0);
        assertEquals(0, zero.feeAmount());
        assertEquals(123, zero.netAmount());
        var full = run("A,T1,123,100,APPROVED\n", 100);
        assertEquals(123, full.feeAmount());
        assertEquals(0, full.netAmount());
    }

    @Test
    void headerOnlyProducesEmptySettlement() throws IOException {
        var result = run("\n", 3);
        assertTrue(result.merchants().isEmpty());
        assertEquals(0, result.approvedTransactions());
        assertEquals(0, result.grossAmount());
        assertEquals(0, result.feeAmount());
        assertEquals(0, result.netAmount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "wrong,header\n"})
    void rejectsMissingOrIncorrectHeader(String csv) throws IOException {
        Path input = Files.writeString(temp.resolve("transactions.csv"), csv);
        assertThrows(IllegalArgumentException.class,
                () -> SettlementBatch.run(input, config(3), output()));
        assertFalse(Files.exists(output()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{\"maxFeeRatePercent\":-1}",
            "{\"maxFeeRatePercent\":101}", "{\"maxFeeRatePercent\":1.5}",
            "{\"maxFeeRatePercent\":\"3\"}", "{\"maxFeeRatePercent\":4294967296}"})
    void rejectsInvalidConfigWithoutWritingOutput(String json) throws IOException {
        Path config = Files.writeString(temp.resolve("config.json"), json);
        assertThrows(IllegalArgumentException.class,
                () -> SettlementBatch.run(Path.of("input/transactions.csv"), config, output()));
        assertFalse(Files.exists(output()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A,T1,1,1", " ,T1,1,1,APPROVED", "A, ,1,1,APPROVED",
            "A,T1,-1,1,APPROVED", "A,T1,1,-1,APPROVED", "A,T1,1,101,APPROVED",
            "A,T1,not-a-number,1,APPROVED", "A,T1,1,1,UNKNOWN", "\"unclosed,T1,1,1,APPROVED"})
    void rejectsMalformedTransactionWithLineNumber(String row) {
        var error = assertThrows(IllegalArgumentException.class, () -> run(row + "\n", 3));
        assertTrue(error.getMessage().startsWith("Invalid CSV line 2:"));
        assertFalse(Files.exists(output()));
    }

    @Test
    void rejectsDuplicateTransactionIdsAcrossMerchantsAndStatuses() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> run("A,T1,100,1,CANCELLED\nB,T1,100,1,APPROVED\n", 3));
        assertTrue(error.getMessage().contains("Duplicate transactionId at CSV line 3"));
        assertFalse(Files.exists(output()));
    }

    @Test
    void rejectsTotalOverflowInsteadOfWritingWrappedAmounts() {
        assertThrows(ArithmeticException.class,
                () -> run("A,T1,9223372036854775807,0,APPROVED\nB,T2,1,0,APPROVED\n", 3));
        assertFalse(Files.exists(output()));
    }

    @Test
    void propagatesMissingInputAndMalformedJsonWithoutWritingOutput() throws IOException {
        Path config = config(3);
        assertThrows(IOException.class, () -> SettlementBatch.run(temp.resolve("missing.csv"), config, output()));
        Files.writeString(config, "{broken");
        assertThrows(IOException.class,
                () -> SettlementBatch.run(Path.of("input/transactions.csv"), config, output()));
        assertFalse(Files.exists(output()));
    }

    @Test
    void rejectsPartialCliArguments() {
        assertThrows(IllegalArgumentException.class, () -> SettlementBatch.main(new String[]{"input.csv"}));
    }

    private SettlementBatch.SettlementResult run(String rows, int maxRate) throws IOException {
        Path input = Files.writeString(temp.resolve("transactions.csv"), HEADER + rows);
        return SettlementBatch.run(input, config(maxRate), output());
    }

    private Path config(int maxRate) throws IOException {
        return Files.writeString(temp.resolve("config.json"), "{\"maxFeeRatePercent\":" + maxRate + "}");
    }

    private Path output() {
        return temp.resolve("settlement.json");
    }
}
