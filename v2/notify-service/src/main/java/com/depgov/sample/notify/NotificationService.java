package com.depgov.sample.notify;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final DeliveryPlanner planner = new DeliveryPlanner();

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification schedule(NotificationRequest request) {
        String recipient = StringUtils.trimToEmpty(request.recipient());
        String message = StringUtils.normalizeSpace(request.message());
        if (StringUtils.isBlank(recipient) || recipient.length() > 120
                || StringUtils.isBlank(message) || message.length() > 1000
                || request.scheduledAt() == null || StringUtils.isBlank(request.partnerBaseUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid notification fields");
        }
        String deliveryUrl;
        try {
            deliveryUrl = planner.deliveryUrl(request.partnerBaseUrl());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid partnerBaseUrl", e);
        }
        Notification notification = new Notification(UUID.randomUUID().toString(), recipient, message,
                request.scheduledAt(), deliveryUrl, "QUEUED");
        repository.save(notification);
        return notification;
    }

    public Notification find(String id) {
        Notification notification = repository.find(id);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found");
        }
        return notification;
    }
}
