package com.depgov.sample.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class NotificationCodec {
    private final ObjectMapper mapper;

    public NotificationCodec() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    public String encode(Notification notification) {
        try {
            return mapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot encode notification", e);
        }
    }

    public Notification decode(String json) {
        try {
            return mapper.readValue(json, Notification.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot decode stored notification", e);
        }
    }
}
