package com.depgov.sample.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class NotificationCodecTest {
    private final NotificationCodec codec = new NotificationCodec();

    @Test
    void roundTripPreservesOffsetAndEscapedMessage() {
        Notification notification = new Notification("n-1", "ops", "hello \"team\"\n한글",
                OffsetDateTime.parse("2030-11-01T09:00:00+09:00"),
                "https://notify.example.invalid/notifications", "QUEUED");
        String json = codec.encode(notification);
        assertTrue(json.contains("2030-11-01T09:00:00+09:00"));
        assertEquals(notification, codec.decode(json));
    }

    @Test
    void corruptStoredJsonFailsClearly() {
        assertThrows(IllegalStateException.class, () -> codec.decode("{broken"));
    }
}
