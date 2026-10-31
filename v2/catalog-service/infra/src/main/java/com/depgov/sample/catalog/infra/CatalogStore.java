package com.depgov.sample.catalog.infra;

import com.depgov.sample.catalog.domain.Product;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogStore {
    private final ProductRepository repository;

    public CatalogStore(ProductRepository repository) {
        this.repository = repository;
    }

    public Product save(Product product) {
        return repository.saveAndFlush(product);
    }

    public Optional<Product> findBySku(String sku) {
        String normalized = StringUtils.upperCase(StringUtils.trim(sku), Locale.ROOT);
        return repository.findById(normalized);
    }
}
