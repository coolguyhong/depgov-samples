// SMP-C25/C26/C27 integration behavior; SMP-C28 remains declaration-only.
package com.depgov.sample.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyReportTest {
    @TempDir
    Path temp;

    @Test
    void sampleMatchesExpectedBytesAndCreatesOutputDirectory() throws IOException {
        Path output = temp.resolve("nested/report.txt");
        LegacyReport.generate(Paths.get("input/sales.txt"), output);
        assertArrayEquals(Files.readAllBytes(Paths.get("expected/report.txt")), Files.readAllBytes(output));
    }

    @Test
    void sortsAcrossYearsAndRoundsMonthlyRatherThanPerSale() throws IOException {
        Path input = input("20260102SEOUL 000000000005", "20251231BUSAN 000000000015",
                "20260101DAEGU 000000000005");
        Path output = temp.resolve("report.txt");
        LegacyReport.generate(input, output);
        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertEquals("2025-12 1 15 2 17", columns(lines.get(2)));
        assertEquals("2026-01 2 10 1 11", columns(lines.get(3)));
        assertEquals("TOTAL 3 25 3 28", columns(lines.get(4)));
    }

    @Test
    void emptyInputProducesZeroTotal() throws IOException {
        Path output = temp.resolve("empty.txt");
        LegacyReport.generate(input(), output);
        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals("TOTAL 0 0 0 0", columns(lines.get(2)));
    }

    @Test
    void acceptsLeapDayAndZeroSales() throws IOException {
        Path output = temp.resolve("zero.txt");
        LegacyReport.generate(input("20240229SEOUL 000000000000"), output);
        assertEquals("2024-02 1 0 0 0",
                columns(Files.readAllLines(output, StandardCharsets.UTF_8).get(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20260229SEOUL 000000000001",
            "20261301SEOUL 000000000001",
            "20260101seoul 000000000001",
            "20260101      000000000001",
            "20260101SEOUL -00000000001",
            "20260101SEOUL 00000000000X",
            "20260101SEOUL 00000000001",
            ""
    })
    void rejectsMalformedRecordsBeforeWriting(String record) throws IOException {
        Path input = input(record);
        Path output = temp.resolve("invalid.txt");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> LegacyReport.generate(input, output));
        assertTrue(error.getMessage().contains("line 1"));
        assertFalse(Files.exists(output));
    }

    @Test
    void invalidSecondRecordPreservesExistingReportAndIdentifiesLine() throws IOException {
        Path input = input("20260101SEOUL 000000000001", "bad record");
        Path output = temp.resolve("existing.txt");
        byte[] previous = "previous report\n".getBytes(StandardCharsets.UTF_8);
        Files.write(output, previous);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> LegacyReport.generate(input, output));
        assertTrue(error.getMessage().contains("line 2"));
        assertArrayEquals(previous, Files.readAllBytes(output));
    }

    @Test
    void missingInputDoesNotCreateOutput() {
        Path output = temp.resolve("missing.txt");
        assertThrows(IOException.class, () -> LegacyReport.generate(temp.resolve("absent.txt"), output));
        assertFalse(Files.exists(output));
    }

    @Test
    void commandLineAcceptsExplicitPaths() throws IOException {
        Path output = temp.resolve("cli.txt");
        LegacyReport.main(new String[] {"input/sales.txt", output.toString()});
        assertArrayEquals(Files.readAllBytes(Paths.get("expected/report.txt")), Files.readAllBytes(output));
    }

    @Test
    void rejectsWrongArgumentCount() {
        assertThrows(IllegalArgumentException.class, () -> LegacyReport.main(new String[] {"input-only"}));
    }

    private Path input(String... lines) throws IOException {
        Path input = temp.resolve("sales.txt");
        Files.write(input, Arrays.asList(lines), StandardCharsets.UTF_8);
        return input;
    }

    private static String columns(String line) {
        return line.trim().replaceAll(" +", " ");
    }
}
