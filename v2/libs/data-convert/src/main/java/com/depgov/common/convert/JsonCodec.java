// SMP-C08, SMP-C12, SMP-C33: 정적 호출용 API; 공유 ObjectMapper를 직접 호출.
package com.depgov.common.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonCodec() {
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read JSON", e);
        }
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write JSON", e);
        }
    }
}
