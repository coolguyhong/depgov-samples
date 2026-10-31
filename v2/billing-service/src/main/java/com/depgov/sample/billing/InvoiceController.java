package com.depgov.sample.billing;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    private static final Logger LOG = LoggerFactory.getLogger(InvoiceController.class);
    private final InvoiceCalculator calculator;
    private final InvoiceRepository repository;

    public InvoiceController(InvoiceCalculator calculator, InvoiceRepository repository) {
        this.calculator = calculator;
        this.repository = repository;
    }

    @PutMapping("/{invoiceId}")
    public Invoice save(@PathVariable("invoiceId") String invoiceId, @RequestBody InvoiceRequest request) {
        Invoice invoice = calculator.calculate(invoiceId, request);
        repository.save(invoice);
        LOG.info("Saved invoice {} total={}", invoice.invoiceId(), invoice.total());
        return invoice;
    }

    @GetMapping("/{invoiceId}")
    public Invoice find(@PathVariable("invoiceId") String invoiceId) {
        Optional<Invoice> invoice = repository.find(calculator.validId(invoiceId));
        if (invoice.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        return invoice.get();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
