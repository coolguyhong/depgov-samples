package com.depgov.sample.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "catalog_products")
public class Product {
    @Id
    @Column(length = 32, nullable = false)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 120)
    private String attachmentName;

    protected Product() {
    }

    public Product(String sku, String name, long price, String attachmentName) {
        String normalizedSku = StringUtils.upperCase(StringUtils.trim(sku), Locale.ROOT);
        String normalizedName = StringUtils.normalizeSpace(name);
        if (normalizedSku == null || !normalizedSku.matches("[A-Z0-9][A-Z0-9-]{0,31}")) {
            throw new IllegalArgumentException("sku must contain 1-32 letters, digits or hyphens");
        }
        if (StringUtils.isBlank(normalizedName) || normalizedName.length() > 100) {
            throw new IllegalArgumentException("name must contain 1-100 characters");
        }
        if (price < 0) {
            throw new IllegalArgumentException("price must be a nonnegative integer");
        }
        if (StringUtils.isBlank(attachmentName) || attachmentName.length() > 120) {
            throw new IllegalArgumentException("attachmentName must contain 1-120 characters");
        }
        this.sku = normalizedSku;
        this.name = normalizedName;
        this.price = price;
        this.attachmentName = attachmentName;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public String getAttachmentName() {
        return attachmentName;
    }
}
