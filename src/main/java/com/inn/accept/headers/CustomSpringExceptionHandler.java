package com.inn.accept.headers;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom exception handler to distinguish between different 406 Not Acceptable scenarios.
 * 
 * Both scenarios throw HttpMediaTypeNotAcceptableException but have different root causes:
 * 1. Element Limit Exceeded: Accept header contains 51+ elements (DoS protection)
 * 2. Media Type Mismatch: Accept header doesn't match endpoint's 'produces' attribute
 */
@ControllerAdvice
public class CustomSpringExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSpringExceptionHandler.class);
    private static final int MAX_ACCEPT_ELEMENTS = 50;
    
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleMediaTypeNotAcceptable(HttpServletRequest req, HttpServletResponse response, 
                                            HttpMediaTypeNotAcceptableException e) throws Exception {
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
            throw e;
        }
        
        response.setStatus(SC_BAD_REQUEST);
        response.setContentType("application/json;charset=utf-8");
        
        String acceptHeader = req.getHeader("Accept");
        String errorDescription;
        String errorType;
        
        // Identify root cause by checking Accept header element count and error message
        if (acceptHeader != null && acceptHeader.split(",").length > MAX_ACCEPT_ELEMENTS) {
            // Scenario 1: Element limit exceeded (Phase 2)
            errorType = "ELEMENT_LIMIT_EXCEEDED";
            errorDescription = String.format("Too many Accept header elements: %d (max allowed: %d)", 
                                           acceptHeader.split(",").length, MAX_ACCEPT_ELEMENTS);
        } else if (e.getMessage() != null && e.getMessage().contains("Too many elements")) {
            // Scenario 1: Caught during internal sorting (Phase 2 - fragility zone)
            errorType = "ELEMENT_LIMIT_EXCEEDED";
            errorDescription = "Accept header parsing failed: Too many elements during content negotiation";
        } else {
            // Scenario 2: Media type mismatch (Phase 3)
            errorType = "MEDIA_TYPE_MISMATCH";
            errorDescription = String.format("Accept header '%s' does not match endpoint's supported media types", 
                                           acceptHeader != null ? acceptHeader : "null");
        }
        
        LOGGER.warn("406 Not Acceptable - Type: {}, Error: {}, Exception: {}", 
                   errorType, errorDescription, e.getMessage());
        
        response.setHeader("X-Error-Type", errorType);
        response.setHeader("X-Error-Description", errorDescription);
        
        String errorMessage = String.format(
            "{\"error\":\"Bad Request\",\"error_type\":\"%s\",\"error_description\":\"%s\"}",
            errorType, errorDescription
        );
        response.getOutputStream().write(errorMessage.getBytes());
    }
}
