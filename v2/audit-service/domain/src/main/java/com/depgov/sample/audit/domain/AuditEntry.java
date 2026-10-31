package com.depgov.sample.audit.domain;

public record AuditEntry(String eventId, String actor, String action, String detail) {
}
