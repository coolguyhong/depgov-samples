package com.depgov.sample.audit.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuditRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String eventId,
        @NotBlank @Size(max = 80) String actor,
        @NotBlank @Pattern(regexp = "(?i)\\s*(create|update|delete|read)\\s*") String action,
        @Size(max = 500) String detail) {
}
