package com.depgov.sample.audit.app;

import com.depgov.sample.audit.domain.AuditEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AuditEntry entry) {
        try {
            String payload = mapper.writeValueAsString(entry);
            jdbcTemplate.update("INSERT INTO audit_event (event_id, payload) VALUES (?, ?)",
                    entry.eventId(), payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode audit entry", exception);
        }
    }

    public AuditEntry find(String eventId) {
        List<String> payloads = jdbcTemplate.queryForList(
                "SELECT payload FROM audit_event WHERE event_id = ?", String.class, eventId);
        if (payloads.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(payloads.get(0), AuditEntry.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot decode audit entry", exception);
        }
    }
}
