package com.depgov.sample.notify;

import java.time.OffsetDateTime;

public record NotificationRequest(String recipient, String message,
                                  OffsetDateTime scheduledAt, String partnerBaseUrl) {
}
