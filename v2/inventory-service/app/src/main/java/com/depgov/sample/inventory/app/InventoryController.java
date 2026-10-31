package com.depgov.sample.inventory.app;

import com.depgov.common.util.StringKit;
import com.depgov.sample.inventory.domain.InventoryPolicy;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventoryController {
    private final InventoryRepository repository;
    private final InventoryRequestValidator validator;
    private final InventoryJson json;

    public InventoryController(InventoryRepository repository, InventoryRequestValidator validator, InventoryJson json) {
        this.repository = repository;
        this.validator = validator;
        this.json = json;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<String> create(@RequestBody CreateInventoryRequest request) {
        validator.validate(request);
        String sku = normalizeSku(request.sku());
        if (repository.existsById(sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "sku already exists");
        }
        String name = StringUtils.normalizeSpace(request.name());
        InventoryItem item = repository.saveAndFlush(new InventoryItem(sku, name, request.quantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(json.write(view(item)));
    }

    @GetMapping("/{sku}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> get(@PathVariable String sku) {
        Optional<InventoryItem> item = repository.findById(normalizeSku(sku));
        if (item.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown sku");
        }
        return ResponseEntity.ok(json.write(view(item.get())));
    }

    private String normalizeSku(String sku) {
        try {
            return InventoryPolicy.normalizeSku(sku);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private InventoryView view(InventoryItem item) {
        String stockCard = StringKit.padRight(item.getSku(), 12) + " | " + item.getName();
        return new InventoryView(item.getSku(), item.getName(), item.getQuantity(),
                InventoryPolicy.stockStatus(item.getQuantity()), stockCard);
    }
}
