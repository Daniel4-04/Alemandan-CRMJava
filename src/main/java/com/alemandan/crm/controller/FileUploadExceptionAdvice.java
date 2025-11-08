package com.alemandan.crm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class FileUploadExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadExceptionAdvice.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSize(MaxUploadSizeExceededException exc) {
        logger.error("MaxUploadSizeExceededException: {}", exc.getMessage(), exc);
        return ResponseEntity.status(413).body("Archivo demasiado grande. Límite alcanzado.");
    }
}