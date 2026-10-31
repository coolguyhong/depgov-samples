package com.depgov.sample.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DeliveryPlannerTest {
    private final DeliveryPlanner planner = new DeliveryPlanner();

    @Test
    void preparesDeliveryUrlWithoutSendingRequest() {
        assertEquals("https://notify.example.invalid/api/notifications",
                planner.deliveryUrl("https://notify.example.invalid/api///"));
        assertEquals("http://notify.example.invalid/notifications",
                planner.deliveryUrl("http://notify.example.invalid"));
    }

    @Test
    void rejectsUnsupportedSchemesQueriesAndFragments() {
        String[] invalid = {"file:///tmp/outbox", "not a URL", "https:///no-host",
                "https://notify.example.invalid?token=x", "https://notify.example.invalid#fragment"};
        for (String url : invalid) {
            assertThrows(IllegalArgumentException.class, () -> planner.deliveryUrl(url), url);
        }
    }

    @Test
    void rejectsUserInfo() {
        assertThrows(IllegalArgumentException.class,
                () -> planner.deliveryUrl("https://user@notify.example.invalid"));
    }
}
