package com.depgov.sample.catalog.app;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yaml.snakeyaml.error.YAMLException;

@RestControllerAdvice
public class CatalogErrors {
    @ExceptionHandler({IllegalArgumentException.class, YAMLException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidInput(Exception exception) {
        return Map.of("error", "Invalid catalog product");
    }
}
