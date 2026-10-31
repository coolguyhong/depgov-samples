package com.depgov.sample.inventory.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApplicationTest {
    private static final String REQUEST = """
            {"sku":" sku-1 ","name":"  Blue   pen  ","quantity":12}
            """;
    private static final String EXPECTED = """
            {"sku":"SKU-1","name":"Blue pen","quantity":12,"status":"IN_STOCK",
             "stockCard":"SKU-1        | Blue pen"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InventoryRepository repository;

    @BeforeEach
    void clearInventory() {
        repository.deleteAll();
    }

    @Test
    void createsAndReadsPersistedNormalizedInventory() throws Exception {
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED));
        assertEquals(1, repository.count());
        InventoryItem persisted = repository.findById("SKU-1").orElseThrow();
        assertEquals("Blue pen", persisted.getName());
        assertEquals(12, persisted.getQuantity());
        mvc.perform(get("/api/inventory/sku-1"))
                .andExpect(status().isOk())
                .andExpect(content().json(EXPECTED));
    }

    @Test
    void zeroQuantityIsOutOfStock() throws Exception {
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"EMPTY\",\"name\":\"Empty bin\",\"quantity\":0}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {"sku":"EMPTY","name":"Empty bin","quantity":0,"status":"OUT_OF_STOCK",
                         "stockCard":"EMPTY        | Empty bin"}
                        """));
    }

    @Test
    void duplicateNormalizedSkuDoesNotOverwriteInventory() throws Exception {
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"name\":\"Replacement\",\"quantity\":99}"))
                .andExpect(status().isConflict());
        assertEquals(1, repository.count());
        assertEquals(12, repository.findById("SKU-1").orElseThrow().getQuantity());
    }

    @Test
    void unknownSkuReturns404() throws Exception {
        mvc.perform(get("/api/inventory/MISSING"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sku\":\"\",\"name\":\"Pen\",\"quantity\":1}",
            "{\"sku\":\"BAD_SKU\",\"name\":\"Pen\",\"quantity\":1}",
            "{\"sku\":\"SKU-1\",\"name\":\"   \",\"quantity\":1}",
            "{\"sku\":\"SKU-1\",\"name\":\"Pen\",\"quantity\":-1}",
            "{\"sku\":\"SKU-1\",\"name\":\"Pen\",\"quantity\":null}",
            "{\"sku\":\"SKU-1\",\"name\":\"Pen\"}",
            "{\"sku\":\"123456789012345678901234567890123\",\"name\":\"Pen\",\"quantity\":1}"
    })
    void rejectsInvalidInputWithoutPersisting(String request) throws Exception {
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    @Test
    void malformedJsonDoesNotPersist() throws Exception {
        mvc.perform(post("/api/inventory").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }
}
