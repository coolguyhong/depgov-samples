package com.depgov.sample.catalog.app;

import com.depgov.common.util.StringKit;
import com.depgov.sample.catalog.domain.Product;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class CatalogYamlReader {
    public Product read(String input) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object document = new Yaml(new SafeConstructor(options)).load(input);
        if (!(document instanceof Map<?, ?> fields)
                || !fields.keySet().equals(Set.of("sku", "name", "price", "attachment"))) {
            throw new IllegalArgumentException("YAML must contain sku, name, price and attachment only");
        }
        String sku = requiredText(fields.get("sku"), "sku");
        String name = requiredText(fields.get("name"), "name");
        String attachment = requiredText(fields.get("attachment"), "attachment");
        Object rawPrice = fields.get("price");
        if (!(rawPrice instanceof Integer) && !(rawPrice instanceof Long)) {
            throw new IllegalArgumentException("price must be a nonnegative 64-bit integer");
        }
        String attachmentName = FilenameUtils.getName(attachment);
        return new Product(sku, name, ((Number) rawPrice).longValue(), attachmentName);
    }

    private String requiredText(Object value, String field) {
        if (!(value instanceof String text) || StringKit.isBlank(text)) {
            throw new IllegalArgumentException(field + " must be nonblank text");
        }
        return text;
    }
}
