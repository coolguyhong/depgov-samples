package com.depgov.sample.catalog.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.depgov.sample.catalog.domain.Product;
import org.junit.jupiter.api.Test;

class CatalogYamlReaderTest {
    private final CatalogYamlReader reader = new CatalogYamlReader();

    @Test
    void stripsWindowsAttachmentPathAndKeepsLongPrice() {
        Product product = reader.read("""
                sku: big-1
                name: Large Product
                price: 9223372036854775807
                attachment: 'C:\\incoming\\photo.png'
                """);
        assertEquals("photo.png", product.getAttachmentName());
        assertEquals(Long.MAX_VALUE, product.getPrice());
    }

    @Test
    void rejectsEmptyListMissingAndUnknownFields() {
        for (String input : new String[]{"", "[]", "sku: A", """
                sku: A
                name: Name
                price: 1
                attachment: a.png
                unknown: value
                """}) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(input));
        }
    }

    @Test
    void rejectsDecimalQuotedBooleanNullAndOverflowPrices() {
        for (String price : new String[]{"1.5", "'100'", "true", "null", "9223372036854775808"}) {
            String input = "sku: A\nname: Name\nattachment: a.png\nprice: " + price;
            assertThrows(IllegalArgumentException.class, () -> reader.read(input));
        }
    }

    @Test
    void rejectsBlankOrNonTextFieldsAndEmptyFilename() {
        for (String sku : new String[]{"' '", "null", "123"}) {
            String input = "sku: " + sku + "\nname: Name\nprice: 1\nattachment: a.png";
            assertThrows(IllegalArgumentException.class, () -> reader.read(input));
        }
        assertThrows(IllegalArgumentException.class,
                () -> reader.read("sku: A\nname: Name\nprice: 1\nattachment: uploads/"));
    }
}
