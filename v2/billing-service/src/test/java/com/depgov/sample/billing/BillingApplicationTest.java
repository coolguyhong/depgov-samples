package com.depgov.sample.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BillingApplicationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path sampleRoot = Path.of(System.getProperty("sample.root"));

    @BeforeEach
    void clearInvoices() {
        jdbcTemplate.update("DELETE FROM invoices");
    }

    @Test
    void samplePutAndGetMatchExpectedAndPersistJsonInH2() throws Exception {
        String input = Files.readString(sampleRoot.resolve("input/invoice.json"));
        String expected = Files.readString(sampleRoot.resolve("expected/invoice.json"));
        String saved = mvc.perform(put("/invoices/INV-001").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String loaded = mvc.perform(get("/invoices/INV-001"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String stored = jdbcTemplate.queryForObject(
                "SELECT document FROM invoices WHERE invoice_id = ?", String.class, "INV-001");
        assertEquals(mapper.readTree(expected), mapper.readTree(saved));
        assertEquals(mapper.readTree(expected), mapper.readTree(loaded));
        assertEquals(mapper.readTree(expected), mapper.readTree(stored));
    }

    @Test
    void repeatedPutReplacesOneInvoice() throws Exception {
        mvc.perform(put("/invoices/REPLACE").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"Before\",\"quantity\":1,\"unitPrice\":100}"))
                .andExpect(status().isOk());
        mvc.perform(put("/invoices/REPLACE").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"After\",\"quantity\":2,\"unitPrice\":200}"))
                .andExpect(status().isOk());
        String loaded = mvc.perform(get("/invoices/REPLACE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Invoice invoice = mapper.readValue(loaded, Invoice.class);
        assertEquals("After", invoice.customer());
        assertEquals(440L, invoice.total());
        assertEquals(1L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class));
    }

    @Test
    void unknownInvoiceReturns404() throws Exception {
        mvc.perform(get("/invoices/UNKNOWN")).andExpect(status().isNotFound());
    }

    @Test
    void invalidInputDoesNotCreateInvoice() throws Exception {
        String response = mvc.perform(put("/invoices/INVALID").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\" \",\"quantity\":1,\"unitPrice\":100}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertEquals("customer is required", mapper.readTree(response).path("error").asText());
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class));
    }

    @Test
    void malformedJsonMissingAmountAndFractionalAmountsReturn400() throws Exception {
        mvc.perform(put("/invoices/INVALID").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/invoices/INVALID").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"A\",\"quantity\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/invoices/INVALID").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"A\",\"quantity\":1.5,\"unitPrice\":100}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/invoices/INVALID").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"A\",\"quantity\":1,\"unitPrice\":100.5}"))
                .andExpect(status().isBadRequest());
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class));
    }

    @Test
    void invalidIdAndOverflowReturn400WithoutReplacingExistingInvoice() throws Exception {
        mvc.perform(get("/invoices/bad.id")).andExpect(status().isBadRequest());
        mvc.perform(put("/invoices/KEEP").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"Original\",\"quantity\":1,\"unitPrice\":100}"))
                .andExpect(status().isOk());
        mvc.perform(put("/invoices/KEEP").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"Overflow\",\"quantity\":1,\"unitPrice\":9223372036854775807}"))
                .andExpect(status().isBadRequest());
        String loaded = mvc.perform(get("/invoices/KEEP"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals("Original", mapper.readValue(loaded, Invoice.class).customer());
        assertEquals(1L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class));
    }
}
