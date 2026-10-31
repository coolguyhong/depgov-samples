package com.depgov.sample.audit.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path sampleRoot = Path.of(System.getProperty("sample.root"));

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM audit_event");
    }

    @Test
    void scenarioCreatesAndReadsNormalizedPersistedJson() throws Exception {
        String input = Files.readString(sampleRoot.resolve("input/audit-event.json"));
        String expected = Files.readString(sampleRoot.resolve("expected/audit-event.json"));
        String created = mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON)
                        .content(input))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertEquals(mapper.readTree(expected), mapper.readTree(created));

        String stored = jdbcTemplate.queryForObject(
                "SELECT payload FROM audit_event WHERE event_id = ?", String.class, "EVT-001");
        assertEquals(mapper.readTree(expected), mapper.readTree(stored));
        String fetched = mvc.perform(get("/audit-events/EVT-001"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(mapper.readTree(expected), mapper.readTree(fetched));
    }

    @Test
    void invalidFieldsAreRejectedBeforeInsert() throws Exception {
        String[] invalidRequests = {
                "{\"eventId\":\"\",\"actor\":\"ops\",\"action\":\"read\"}",
                "{\"eventId\":\"bad/id\",\"actor\":\"ops\",\"action\":\"read\"}",
                "{\"eventId\":\"E1\",\"actor\":\"  \",\"action\":\"read\"}",
                mapper.writeValueAsString(new AuditRequest("E1", "\u00a0", "read", "")),
                "{\"eventId\":\"E1\",\"actor\":\"ops\",\"action\":\"export\"}",
                "{\"eventId\":\"E1\",\"actor\":\"ops\"}",
                mapper.writeValueAsString(new AuditRequest("E1", "x".repeat(81), "read", "")),
                mapper.writeValueAsString(new AuditRequest("E1", "ops", "read", "x".repeat(501)))
        };
        for (String input : invalidRequests) {
            mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON).content(input))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, countEvents());
    }

    @Test
    void duplicateIdReturnsConflictAndPreservesOriginal() throws Exception {
        String original = "{\"eventId\":\"E1\",\"actor\":\"first\",\"action\":\"create\"}";
        mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON).content(original))
                .andExpect(status().isCreated());
        mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"E1\",\"actor\":\"second\",\"action\":\"delete\"}"))
                .andExpect(status().isConflict());
        assertEquals(1, countEvents());
        String fetched = mvc.perform(get("/audit-events/E1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals("first", mapper.readTree(fetched).path("actor").asText());
        assertEquals("CREATE", mapper.readTree(fetched).path("action").asText());
    }

    @Test
    void missingEventReturnsNotFound() throws Exception {
        mvc.perform(get("/audit-events/unknown")).andExpect(status().isNotFound());
        assertEquals(0, countEvents());
    }

    @Test
    void emptyNullAndMalformedBodiesReturnBadRequest() throws Exception {
        for (String input : new String[]{"", "null", "{}", "{broken"}) {
            mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON).content(input))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, countEvents());
    }

    @Test
    void optionalDetailBecomesEmptyString() throws Exception {
        String created = mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"E2\",\"actor\":\"ops\",\"action\":\"read\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertEquals("", mapper.readTree(created).path("detail").asText());
    }

    @Test
    void maximumLengthAndEscapedDetailRoundTrip() throws Exception {
        AuditRequest input = new AuditRequest("E".repeat(40), "a".repeat(80), "read",
                "x" + "\u0001".repeat(498) + "x");
        mvc.perform(post("/audit-events").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isCreated());
        String fetched = mvc.perform(get("/audit-events/" + input.eventId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(input.detail(), mapper.readTree(fetched).path("detail").asText());
    }

    private int countEvents() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class);
    }
}
