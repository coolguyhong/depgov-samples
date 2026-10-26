// SMP-C08, SMP-C12: 정적 CSV 호출; 따옴표·빈 필드·잘못된 따옴표 경계.
package com.depgov.common.convert;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvLineTest {
    @Test
    void splitsAndTrimsFields() {
        assertEquals(List.of("a", "b", "c"), CsvLine.split(" a, b ,c "));
    }

    @Test
    void supportsQuotedCommasAndEscapedQuotes() {
        assertEquals(List.of("a,b", "say \"hi\"", "tail"),
                CsvLine.split(" \"a,b\" ,\"say \"\"hi\"\"\",tail"));
        assertEquals(List.of("trim"), CsvLine.split("\" trim \""));
    }

    @Test
    void preservesEmptyFieldsAndHandlesNullLine() {
        assertEquals(List.of(), CsvLine.split(null));
        assertEquals(List.of(""), CsvLine.split(""));
        assertEquals(List.of("", "", ""), CsvLine.split(",,"));
        assertEquals(List.of("", ""), CsvLine.split("\"\","));
    }

    @Test
    void quotesSpecialFieldsAndRoundTrips() {
        List<String> fields = List.of("a,b", "say \"hi\"", "line\nbreak", "carriage\rreturn", "");
        assertEquals("\"a,b\",\"say \"\"hi\"\"\",\"line\nbreak\",\"carriage\rreturn\",", CsvLine.join(fields));
        assertEquals(fields, CsvLine.split(CsvLine.join(fields)));
    }

    @Test
    void joinsNullEmptyAndOrdinaryFields() {
        assertEquals("", CsvLine.join(null));
        assertEquals("", CsvLine.join(List.of()));
        assertEquals("a,,b", CsvLine.join(Arrays.asList("a", null, "b")));
        assertEquals("a,b", CsvLine.join(List.of("a", "b")));
    }

    @Test
    void rejectsMalformedQuotes() {
        assertThrows(IllegalArgumentException.class, () -> CsvLine.split("\"unclosed"));
        assertThrows(IllegalArgumentException.class, () -> CsvLine.split("un\"quoted"));
        assertThrows(IllegalArgumentException.class, () -> CsvLine.split("\"closed\"tail"));
    }
}
