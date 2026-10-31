package com.depgov.sample.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public InvoiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Invoice invoice) {
        try {
            String document = mapper.writeValueAsString(invoice);
            jdbcTemplate.update("MERGE INTO invoices (invoice_id, document) KEY (invoice_id) VALUES (?, ?)",
                    invoice.invoiceId(), document);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot encode invoice", exception);
        }
    }

    public Optional<Invoice> find(String invoiceId) {
        List<String> documents = jdbcTemplate.queryForList(
                "SELECT document FROM invoices WHERE invoice_id = ?", String.class, invoiceId);
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(documents.get(0), Invoice.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot decode stored invoice", exception);
        }
    }
}
