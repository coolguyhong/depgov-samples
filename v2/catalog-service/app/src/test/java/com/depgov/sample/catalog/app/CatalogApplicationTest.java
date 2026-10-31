package com.depgov.sample.catalog.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.depgov.sample.catalog.infra.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogApplicationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository repository;

    private final Path sampleRoot = Path.of(System.getProperty("sample.root"));

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void scenarioRegistersAndReadsPersistedProduct() throws Exception {
        String input = Files.readString(sampleRoot.resolve("input/product.yaml"));
        String expected = Files.readString(sampleRoot.resolve("expected/product.json"));
        mvc.perform(post("/catalog/products").contentType("application/yaml").content(input))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
        assertEquals(1, repository.count());
        mvc.perform(get("/catalog/products/cat-001"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
    }

    @Test
    void missingProductReturns404() throws Exception {
        mvc.perform(get("/catalog/products/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameNormalizedSkuUpdatesOneRow() throws Exception {
        mvc.perform(post("/catalog/products").contentType("text/yaml").content("""
                sku: cat-002
                name: Cup
                price: 100
                attachment: cup.png
                """))
                .andExpect(status().isOk());
        mvc.perform(post("/catalog/products").contentType("text/yaml").content("""
                sku: CAT-002
                name: New Cup
                price: 0
                attachment: new-cup.png
                """))
                .andExpect(status().isOk());
        assertEquals(1, repository.count());
        mvc.perform(get("/catalog/products/cat-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Cup"))
                .andExpect(jsonPath("$.price").value(0))
                .andExpect(jsonPath("$.attachmentName").value("new-cup.png"));
    }

    @Test
    void invalidProductReturns400AndDoesNotWrite() throws Exception {
        mvc.perform(post("/catalog/products").contentType("application/yaml").content("""
                sku: CAT-003
                name: Invalid Price
                price: -1
                attachment: image.png
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid catalog product"));
        assertEquals(0, repository.count());
    }

    @Test
    void malformedDuplicateAndTaggedYamlReturn400() throws Exception {
        for (String input : new String[]{"sku: [", "sku: A\nsku: B\n",
                "!!java.net.URL ['https://invalid.example']", "[]"}) {
            mvc.perform(post("/catalog/products").contentType("application/yaml").content(input))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, repository.count());
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mvc.perform(post("/catalog/products").contentType("application/json").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        assertEquals(0, repository.count());
    }
}
