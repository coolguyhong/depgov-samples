package com.depgov.sample.audit.app;

import com.depgov.sample.audit.domain.AuditEntry;
import com.depgov.sample.audit.domain.AuditNormalizer;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditService implements DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();
    private final AuditNormalizer normalizer = new AuditNormalizer();
    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public AuditEntry create(AuditRequest request) {
        if (request == null || !validator.validate(request).isEmpty()
                || StringUtils.isBlank(StringUtils.normalizeSpace(request.actor()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit request");
        }
        AuditEntry entry = normalizer.normalize(new AuditEntry(
                request.eventId(), request.actor(), request.action(), request.detail()));
        try {
            repository.insert(entry);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "eventId already exists", exception);
        }
        LOG.info("Stored audit event {} action={}", entry.eventId(), entry.action());
        return entry;
    }

    public AuditEntry find(String eventId) {
        if (StringUtils.isBlank(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }
        AuditEntry entry = repository.find(StringUtils.strip(eventId));
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit event not found");
        }
        return entry;
    }

    @Override
    public void destroy() {
        validatorFactory.close();
    }
}
