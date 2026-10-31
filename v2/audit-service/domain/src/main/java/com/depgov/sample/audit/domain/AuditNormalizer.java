package com.depgov.sample.audit.domain;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public final class AuditNormalizer {
    public AuditEntry normalize(AuditEntry entry) {
        Validate.notNull(entry, "entry is required");
        String eventId = StringUtils.strip(entry.eventId());
        String actor = StringUtils.normalizeSpace(entry.actor());
        String action = StringUtils.upperCase(StringUtils.strip(entry.action()), Locale.ROOT);
        String detail = StringUtils.normalizeSpace(entry.detail());
        Validate.isTrue(StringUtils.isNotBlank(eventId), "eventId is required");
        Validate.isTrue(StringUtils.isNotBlank(actor), "actor is required");
        Validate.isTrue(StringUtils.isNotBlank(action), "action is required");
        return new AuditEntry(eventId, actor, action, StringUtils.defaultString(detail));
    }
}
