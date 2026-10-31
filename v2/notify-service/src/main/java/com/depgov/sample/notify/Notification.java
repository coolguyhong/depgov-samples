package com.depgov.sample.notify;

import java.time.OffsetDateTime;

public record Notification(String id, String recipient, String message,
                           OffsetDateTime scheduledAt, String deliveryUrl, String status) {
}
