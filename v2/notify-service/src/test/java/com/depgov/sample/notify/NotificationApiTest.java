package com.depgov.sample.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    private final Path root = Path.of(System.getProperty("sample.root"));

    @BeforeEach
    void clearOutbox() {
        jdbc.update("delete from notification_outbox");
    }

    @Test
    void scenarioPersistsAndRetrievesNormalizedNotification() throws Exception {
        String input = Files.readString(root.resolve("input/notification.json"));
        MvcResult created = post(input);
        assertEquals(201, created.getResponse().getStatus());
        ObjectNode actual = (ObjectNode) mapper.readTree(created.getResponse().getContentAsString());
        String id = actual.remove("id").asText();
        assertNotNull(UUID.fromString(id));
        JsonNode expected = mapper.readTree(Files.readString(root.resolve("expected/notification.json")));
        assertEquals(expected, actual);
        assertEquals("/notifications/" + id, created.getResponse().getHeader("Location"));

        assertEquals(1, jdbc.queryForObject("select count(*) from notification_outbox", Integer.class));
        String stored = jdbc.queryForObject("select payload from notification_outbox where id = ?", String.class, id);
        assertTrue(stored.contains("Release ready"));
        MvcResult retrieved = mvc.perform(MockMvcRequestBuilders.get("/notifications/{id}", id)).andReturn();
        assertEquals(200, retrieved.getResponse().getStatus());
        assertEquals(mapper.readTree(created.getResponse().getContentAsString()),
                mapper.readTree(retrieved.getResponse().getContentAsString()));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        assertEquals(404, mvc.perform(MockMvcRequestBuilders.get("/notifications/missing"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void invalidFieldsDoNotWriteRows() throws Exception {
        String[] fields = {"recipient", "message", "scheduledAt", "partnerBaseUrl"};
        for (String field : fields) {
            ObjectNode input = fixture();
            input.putNull(field);
            assertEquals(400, post(input.toString()).getResponse().getStatus(), field);
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_outbox", Integer.class));
    }

    @Test
    void blankAndOversizedMessagesAreRejected() throws Exception {
        String[] messages = {" \t\n ", "x".repeat(1001)};
        for (String message : messages) {
            ObjectNode input = fixture();
            input.put("message", message);
            assertEquals(400, post(input.toString()).getResponse().getStatus());
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_outbox", Integer.class));
    }

    @Test
    void invalidPartnerAndMalformedDateReturn400() throws Exception {
        ObjectNode input = fixture();
        input.put("partnerBaseUrl", "file:///tmp/outbox");
        assertEquals(400, post(input.toString()).getResponse().getStatus());
        input = fixture();
        input.put("scheduledAt", "tomorrow");
        assertEquals(400, post(input.toString()).getResponse().getStatus());
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_outbox", Integer.class));
    }

    @Test
    void sqlMetacharactersRemainData() throws Exception {
        ObjectNode input = fixture();
        input.put("recipient", "ops'; drop table notification_outbox; --");
        MvcResult created = post(input.toString());
        assertEquals(201, created.getResponse().getStatus());
        assertEquals(1, jdbc.queryForObject("select count(*) from notification_outbox", Integer.class));
        String id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        MvcResult retrieved = mvc.perform(MockMvcRequestBuilders.get("/notifications/{id}", id)).andReturn();
        assertEquals(input.get("recipient"), mapper.readTree(retrieved.getResponse().getContentAsString()).get("recipient"));
    }

    private ObjectNode fixture() throws Exception {
        return (ObjectNode) mapper.readTree(Files.readString(root.resolve("input/notification.json")));
    }

    private MvcResult post(String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/notifications")
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }
}
