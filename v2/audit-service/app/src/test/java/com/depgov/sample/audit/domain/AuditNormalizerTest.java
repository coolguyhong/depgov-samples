package com.depgov.sample.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuditNormalizerTest {
    private final AuditNormalizer normalizer = new AuditNormalizer();

    @Test
    void normalizesWhitespaceAndAction() {
        AuditEntry input = new AuditEntry(" E1 ", "  ops\t user ", " update ", " changed\n retention ");
        assertEquals(new AuditEntry("E1", "ops user", "UPDATE", "changed retention"),
                normalizer.normalize(input));
    }

    @Test
    void preservesUnicodeAndConvertsNullDetailToEmpty() {
        assertEquals(new AuditEntry("E1", "감사 담당자", "READ", ""),
                normalizer.normalize(new AuditEntry("E1", " 감사  담당자 ", "read", null)));
    }

    @Test
    void rejectsNullEntry() {
        assertThrows(NullPointerException.class, () -> normalizer.normalize(null));
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class,
                () -> normalizer.normalize(new AuditEntry(" ", "ops", "read", "")));
        assertThrows(IllegalArgumentException.class,
                () -> normalizer.normalize(new AuditEntry("E1", "\t", "read", "")));
        assertThrows(IllegalArgumentException.class,
                () -> normalizer.normalize(new AuditEntry("E1", "ops", null, "")));
    }
}
