package com.depgov.sample.notify;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Notification> schedule(@RequestBody NotificationRequest request) {
        Notification notification = service.schedule(request);
        return ResponseEntity.created(URI.create("/notifications/" + notification.id())).body(notification);
    }

    @GetMapping("/{id}")
    public Notification find(@PathVariable String id) {
        return service.find(id);
    }
}
