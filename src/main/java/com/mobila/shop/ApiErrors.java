package com.mobila.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erori API în format JSON uniform ({"errors":[...]}), fără stack trace către client. */
@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable(HttpMessageNotReadableException e) {
        return Util.err(400, "Cerere invalidă.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> badParam(MethodArgumentTypeMismatchException e) {
        return Util.err(400, "Parametru invalid.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException e) {
        LOG.warn("Date nesalvate (valoare prea lungă sau duplicat): {}", e.getMessage());
        return Util.err(409, "Datele nu au putut fi salvate (valoare prea lungă sau deja existentă).");
    }
}
