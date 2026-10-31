package com.depgov.sample.catalog.infra;

import com.depgov.sample.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
