package com.depgov.sample.catalog.app;

import com.depgov.sample.catalog.domain.Product;
import com.depgov.sample.catalog.infra.CatalogStore;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/catalog/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {
    private final CatalogStore store;
    private final CatalogYamlReader reader = new CatalogYamlReader();

    public CatalogController(CatalogStore store) {
        this.store = store;
    }

    @PostMapping(consumes = {"application/yaml", "text/yaml", MediaType.TEXT_PLAIN_VALUE})
    public Product register(@RequestBody String yaml) {
        return store.save(reader.read(yaml));
    }

    @GetMapping("/{sku}")
    public Product find(@PathVariable("sku") String sku) {
        Optional<Product> product = store.findBySku(sku);
        if (product.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU");
        }
        return product.get();
    }
}
